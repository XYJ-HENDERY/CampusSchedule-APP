package com.example.campusschedule.util

import com.example.campusschedule.data.model.Course
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * 解析教务系统导出的表格类文件,统一输出「行×列」文本网格。
 * 支持:xlsx、docx、XML Spreadsheet 2003、HTML/网页伪装 xls、CSV/TSV。
 */
object ScheduleImporter {

    class ImportFormatException(msg: String) : Exception(msg)

    private val palette = listOf(
        0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFFFB8C00.toInt(),
        0xFF8E24AA.toInt(), 0xFFE53935.toInt(), 0xFFFDD835.toInt()
    )
    private val cnDigits = mapOf(
        '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
        '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10
    )

    fun parse(bytes: ByteArray): List<Course> = buildCourses(parseGrid(bytes))

    fun parseGrid(bytes: ByteArray): List<List<String>> {
        if (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            val names = ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                generateSequence { z.nextEntry }.map { it.name }.toList()
            }
            return if ("word/document.xml" in names) parseDocx(bytes) else parseXlsx(bytes)
        }
        val isOle = bytes.size >= 8 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0xE1.toByte()
        if (isOle) {
            throw ImportFormatException("该文件是旧版 Word/Excel 97-2003 格式,请另存为 .docx / .xlsx 后导入")
        }
        var text = String(bytes, Charsets.UTF_8)
        if (text.contains('\uFFFD')) text = String(bytes, Charset.forName("GBK"))
        return if (text.contains("mso-application") || text.contains("<Workbook")) {
            parseXmlSpreadsheet(text)
        } else if (Jsoup.parse(text).select("table").firstOrNull { it.select("tr").size >= 2 } != null) {
            parseHtml(text)
        } else {
            parseDelimited(text)
        }
    }

    // ---------- docx ----------
    private fun parseDocx(bytes: ByteArray): List<List<String>> {
        var docXml = ""
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    docXml = zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        if (docXml.isEmpty()) throw ImportFormatException("docx 文件中未找到文档内容")

        val tableRegex = Regex("<(?:[A-Za-z]+:)?tbl(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z]+:)?tbl>", RegexOption.DOT_MATCHES_ALL)
        val rowRegex = Regex("<(?:[A-Za-z]+:)?tr(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z]+:)?tr>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<(?:[A-Za-z]+:)?tc(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z]+:)?tc>", RegexOption.DOT_MATCHES_ALL)

        fun cellText(body: String): String {
            val s = body
                .replace(Regex("<(?:[A-Za-z]+:)?t(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z]+:)?t>", RegexOption.DOT_MATCHES_ALL), "$1")
                .replace(Regex("<(?:[A-Za-z]+:)?tab(?:\\s[^>]*)?/>"), "\t")
                .replace(Regex("<(?:[A-Za-z]+:)?br(?:\\s[^>]*)?/>"), "\n")
                .replace(Regex("</(?:[A-Za-z]+:)?p>"), "\n")
                .replace(Regex("<[^>]+>"), "")
            return decodeEntities(s).trim()
        }

        val tables = tableRegex.findAll(docXml).map { it.groupValues[1] }.toList()
        if (tables.isNotEmpty()) {
            val best = tables.maxByOrNull { t -> rowRegex.findAll(t).count() }!!
            val rows = rowRegex.findAll(best).map { rm ->
                cellRegex.findAll(rm.groupValues[1]).map { cm -> cellText(cm.groupValues[1]) }.toList()
            }.toList()
            if (rows.isNotEmpty() && rows.any { it.any { c -> c.isNotEmpty() } }) return rows
        }

        val paraRegex = Regex("<(?:[A-Za-z]+:)?p(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z]+:)?p>", RegexOption.DOT_MATCHES_ALL)
        val lines = paraRegex.findAll(docXml)
            .map { cellText(it.groupValues[1]) }
            .flatMap { it.split("\n") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.split("\t").map { s -> s.trim() } }
            .toList()
        if (lines.isEmpty()) throw ImportFormatException("docx 文件内容为空")
        return lines
    }

    // ---------- xlsx ----------
    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        var sharedXml = ""
        var sheetXml = ""
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" ->
                        sharedXml = zip.readBytes().toString(Charsets.UTF_8)
                    entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) && sheetXml.isEmpty() ->
                        sheetXml = zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        if (sheetXml.isEmpty()) throw ImportFormatException("xlsx 文件中未找到工作表数据")

        val shared = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(sharedXml).map { si ->
            Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(si.groupValues[1])
                .joinToString("") { decodeEntities(it.groupValues[1]) }
        }.toList()

        val cellRefRegex = Regex("r=\"([A-Z]+)\\d+\"")
        val vRegex = Regex("<v[^>]*>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c([^>]*?)(?:/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL)

        val rows = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            .findAll(sheetXml).map { rm ->
                val cells = mutableListOf<String>()
                for (cm in cellRegex.findAll(rm.groupValues[1])) {
                    val attrs = cm.groupValues[1]
                    val body = cm.groupValues[2]
                    val colIdx = cellRefRegex.find(attrs)?.groupValues?.get(1)?.fold(0) { acc, c ->
                        acc * 26 + (c - 'A' + 1)
                    }?.minus(1) ?: cells.size
                    val text = if (Regex("t=\"s\"").containsMatchIn(attrs)) {
                        vRegex.find(body)?.groupValues?.get(1)?.toIntOrNull()
                            ?.let { shared.getOrElse(it) { "" } } ?: ""
                    } else {
                        vRegex.find(body)?.groupValues?.get(1)?.let { decodeEntities(it) }
                            ?: tRegex.findAll(body).joinToString("") { decodeEntities(it.groupValues[1]) }
                    }
                    while (cells.size < colIdx) cells += ""
                    cells.add(text)
                }
                cells
            }.toList()

        if (rows.size < 2) throw ImportFormatException("工作表中未找到课表数据")
        return rows
    }

    // ---------- XML Spreadsheet 2003 ----------
    private fun parseXmlSpreadsheet(xml: String): List<List<String>> {
        val rowRegex = Regex("<(?:[A-Za-z]+:)?Row[^>]*>(.*?)</(?:[A-Za-z]+:)?Row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<(?:[A-Za-z]+:)?Cell([^>]*?)(?:/>|>(.*?)</(?:[A-Za-z]+:)?Cell>)", RegexOption.DOT_MATCHES_ALL)
        val dataRegex = Regex("<(?:[A-Za-z]+:)?Data[^>]*>(.*?)</(?:[A-Za-z]+:)?Data>", RegexOption.DOT_MATCHES_ALL)
        val indexRegex = Regex("Index\\s*=\\s*\"(\\d+)\"")

        val rows = rowRegex.findAll(xml).map { rowBody ->
            val cells = mutableListOf<String>()
            var nextIdx = 1
            for (cm in cellRegex.findAll(rowBody.groupValues[1])) {
                val attrs = cm.groupValues[1]
                val body = cm.groupValues[2]
                val at = indexRegex.find(attrs)?.groupValues?.get(1)?.toInt() ?: nextIdx
                while (cells.size < at - 1) cells += ""
                val text = dataRegex.findAll(body)
                    .joinToString("\n") { decodeEntities(it.groupValues[1]).trim() }
                if (cells.size < at) cells.add(text)
                nextIdx = cells.size + 1
            }
            cells
        }.toList()
        if (rows.size < 2) throw ImportFormatException("文件中未找到课表表格,请确认选择的是教务导出的课表文件")
        return rows
    }

    // ---------- HTML / CSV ----------
    private fun parseHtml(html: String): List<List<String>> {
        val table = Jsoup.parse(html).select("table")
            .firstOrNull { it.select("tr").size >= 2 }
            ?: throw ImportFormatException("文件中未找到课表表格,请确认选择的是教务导出的课表文件")
        return table.select("tr").map { tr ->
            tr.select("td,th").map { cell ->
                cell.html()
                    .replace(Regex("(?i)<br\\s*/?>(\\r?\\n)?"), "\n")
                    .replace(Regex("<[^>]+>"), "")
                    .let { Jsoup.parse(it).wholeText() }
            }
        }
    }

    private fun parseDelimited(text: String): List<List<String>> {
        val sep = if (text.lineSequence().any { it.contains('\t') }) '\t' else ','
        val rows = text.lines().filter { it.isNotBlank() }
            .map { l -> l.split(sep).map { it.trim().removeSurrounding("\"") } }
        if (rows.size < 2) throw ImportFormatException("无法识别文件格式,请用记事本打开该文件,把前几行内容反馈给我")
        return rows
    }

    private fun decodeEntities(s: String): String {
        var out = s.replace("&#10;", "\n").replace("&#xA;", "\n").replace("&#13;", "")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        out = Regex("&#(\\d+);").replace(out) { it.groupValues[1].toInt().toChar().toString() }
        out = Regex("&#x([0-9a-fA-F]+);").replace(out) { it.groupValues[1].toInt(16).toChar().toString() }
        return out.replace("&amp;", "&")
    }

    // ---------- 通用网格 → 课程(课表导入用) ----------
    private fun dayIndexOf(t: String): Int {
        val m = Regex("(?:星期|周|礼拜)([一二三四五六日天])").find(t) ?: return -1
        val ch = if (m.groupValues[1] == "天") "日" else m.groupValues[1]
        return "一二三四五六日".indexOf(ch)
    }

    private fun buildCourses(grid: List<List<String>>): List<Course> {
        var headerRow = -1
        val dayCols = linkedMapOf<Int, Int>()
        for ((ri, row) in grid.withIndex()) {
            dayCols.clear()
            row.forEachIndexed { ci, t ->
                val di = dayIndexOf(t)
                if (di >= 0 && t.trim().length <= 6) dayCols[ci] = di + 1
            }
            if (dayCols.size >= 5) { headerRow = ri; break }
        }
        if (headerRow < 0) throw ImportFormatException("未找到「星期一~星期日」表头,无法识别课表结构")

        val courses = mutableListOf<Course>()
        var colorIdx = 0
        for (row in grid.drop(headerRow + 1)) {
            if (row.isEmpty()) continue
            val sections = parseSections(row[0]) ?: continue
            val (startSec, endSec) = sections
            if (startSec !in 1..10 || endSec !in 1..10 || endSec < startSec) continue

            for ((ci, day) in dayCols) {
                if (ci == 0 || ci >= row.size) continue
                val cellText = row[ci]
                if (cellText.isBlank()) continue
                for (c in parseCell(cellText, day, startSec, endSec)) {
                    courses += c.copy(color = palette[colorIdx % palette.size])
                    colorIdx++
                }
            }
        }
        if (courses.isEmpty()) throw ImportFormatException("未解析出任何课程,请确认文件内容是本学期的个人课表")
        return courses
    }

    private fun parseSections(label: String): Pair<Int, Int>? {
        val s = label.trim()
        Regex("(\\d{1,2})\\s*[-~－—]\\s*(\\d{1,2})").find(s)?.let {
            return it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }
        Regex("(\\d{1,2})").find(s)?.let { val n = it.groupValues[1].toInt(); return n to n }
        val cn = Regex("第([一二三四五六七八九十]{1,2})节").find(s)?.groupValues?.get(1) ?: return null
        val nums = cn.mapNotNull { cnDigits[it] }
        if (nums.isEmpty()) return null
        return nums.first() to nums.last()
    }

    private fun parseCell(cellText: String, day: Int, startSec: Int, endSec: Int): List<Course> {
        val lines = cellText.lines().map { it.trim() }.filter { it.isNotEmpty() && it != "-" && it != "/" }
        val result = mutableListOf<Course>()

        var name: String? = null
        var teacher = ""
        var sw = -1; var ew = -1

        for (line in lines) {
            val weekIdx = line.indexOf("周")
            val weekPart = if (weekIdx > 0) line.substring(0, weekIdx) else null
            if (weekPart != null && Regex("\\d").containsMatchIn(weekPart) && !Regex("^\\D").containsMatchIn(weekPart)) {
                Regex("(\\d{1,2})(?:\\s*[-~－—]\\s*(\\d{1,2}))?").findAll(weekPart).forEach { m ->
                    val a = m.groupValues[1].toInt()
                    val b = m.groupValues[2].toIntOrNull() ?: a
                    if (sw == -1 || a < sw) sw = a
                    if (ew == -1 || b > ew) ew = b
                }
            } else if (Regex("楼|室|场|馆|中心").containsMatchIn(line) && line.any { it.isDigit() }) {
                if (name != null) {
                    result += Course(
                        name = name, teacher = teacher, classroom = line,
                        dayOfWeek = day, startSection = startSec, endSection = endSec,
                        startWeek = if (sw == -1) 1 else sw,
                        endWeek = if (ew == -1) 20 else ew.coerceAtMost(20),
                        color = 0, type = "导入"
                    )
                }
                name = null; teacher = ""; sw = -1; ew = -1
            } else if (name == null) {
                name = line.replace(Regex("^[0-9A-Za-z]{2,}\\s*"), "")
            } else if (teacher.isEmpty() && Regex("^[\\u4e00-\\u9fa5·]{2,4}$").matches(line)) {
                teacher = line
            }
        }
        return result
    }
}