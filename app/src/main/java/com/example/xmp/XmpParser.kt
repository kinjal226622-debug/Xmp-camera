package com.example.xmp

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object XmpParser {

    private val SUPPORTED_PARAMS = setOf(
        "Exposure2012", "Exposure", "Contrast2012", "Contrast",
        "Highlights2012", "Highlights", "Shadows2012", "Shadows",
        "Whites2012", "Whites", "Blacks2012", "Blacks",
        "Temperature", "Tint", "Vibrance", "Saturation",
        "Clarity2012", "Clarity", "Dehaze",
        "PostCropVignetteAmount", "GrainAmount",
        "SplitToningShadowHue", "SplitToningShadowSaturation",
        "SplitToningHighlightHue", "SplitToningHighlightSaturation",
        "SplitToningBalance", "ColorGradeShadowHue", "ColorGradeShadowSat",
        "ColorGradeHighlightHue", "ColorGradeHighlightSat",
        "ToneCurvePV2012", "ToneCurve"
    )

    private val KNOWN_UNSUPPORTED = setOf(
        "LensProfileEnable", "UprightTransform", "RetouchAreas",
        "PerspectiveVertical", "PerspectiveHorizontal", "PerspectiveRotate",
        "Defringe", "SharpenRadius", "SharpenDetail", "LuminanceSmoothing",
        "ColorNoiseReduction", "ChromaticAberration", "AutoLateralCA"
    )

    /**
     * Parses an XMP preset string and extracts develop parameters.
     */
    fun parse(xmlContent: String, fallbackName: String = "Custom Preset"): XmpPreset {
        var presetName = fallbackName
        var exposure = 0f
        var contrast = 0f
        var highlights = 0f
        var shadows = 0f
        var whites = 0f
        var blacks = 0f
        var temperature = 0f
        var tint = 0f
        var vibrance = 0f
        var saturation = 0f
        var clarity = 0f
        var dehaze = 0f
        var vignette = 0f
        var grain = 0f
        var shadowHue = 0f
        var shadowSat = 0f
        var highlightHue = 0f
        var highlightSat = 0f
        var balance = 0f
        val toneCurvePoints = mutableListOf<Pair<Float, Float>>()
        val unsupportedFound = mutableSetOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTag = ""
            var insideToneCurve = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        val prefix = parser.prefix ?: ""
                        currentTag = name

                        // Check attributes on tags like <rdf:Description crs:Exposure2012="+0.5" ... />
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrVal = parser.getAttributeValue(i)

                            when (attrName) {
                                "PresetName", "presetName", "name" -> if (attrVal.isNotBlank()) presetName = attrVal
                                "Exposure2012", "Exposure" -> exposure = parseSignFloat(attrVal)
                                "Contrast2012", "Contrast" -> contrast = parseSignFloat(attrVal)
                                "Highlights2012", "Highlights" -> highlights = parseSignFloat(attrVal)
                                "Shadows2012", "Shadows" -> shadows = parseSignFloat(attrVal)
                                "Whites2012", "Whites" -> whites = parseSignFloat(attrVal)
                                "Blacks2012", "Blacks" -> blacks = parseSignFloat(attrVal)
                                "Temperature" -> temperature = parseSignFloat(attrVal)
                                "Tint" -> tint = parseSignFloat(attrVal)
                                "Vibrance" -> vibrance = parseSignFloat(attrVal)
                                "Saturation" -> saturation = parseSignFloat(attrVal)
                                "Clarity2012", "Clarity" -> clarity = parseSignFloat(attrVal)
                                "Dehaze" -> dehaze = parseSignFloat(attrVal)
                                "PostCropVignetteAmount" -> vignette = parseSignFloat(attrVal)
                                "GrainAmount" -> grain = parseSignFloat(attrVal)
                                "SplitToningShadowHue", "ColorGradeShadowHue" -> shadowHue = parseSignFloat(attrVal)
                                "SplitToningShadowSaturation", "ColorGradeShadowSat" -> shadowSat = parseSignFloat(attrVal)
                                "SplitToningHighlightHue", "ColorGradeHighlightHue" -> highlightHue = parseSignFloat(attrVal)
                                "SplitToningHighlightSaturation", "ColorGradeHighlightSat" -> highlightSat = parseSignFloat(attrVal)
                                "SplitToningBalance" -> balance = parseSignFloat(attrVal)
                                else -> {
                                    if (attrName in KNOWN_UNSUPPORTED || (prefix == "crs" && attrName !in SUPPORTED_PARAMS)) {
                                        unsupportedFound.add(attrName)
                                    }
                                }
                            }
                        }

                        if (name.contains("ToneCurve", ignoreCase = true)) {
                            insideToneCurve = true
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            if (insideToneCurve && (currentTag == "li" || currentTag == "rdf:li")) {
                                parseToneCurvePoint(text)?.let { toneCurvePoints.add(it) }
                            } else {
                                when (currentTag) {
                                    "PresetName", "presetName" -> if (text.isNotBlank()) presetName = text
                                    "Exposure2012", "Exposure" -> exposure = parseSignFloat(text)
                                    "Contrast2012", "Contrast" -> contrast = parseSignFloat(text)
                                    "Highlights2012", "Highlights" -> highlights = parseSignFloat(text)
                                    "Shadows2012", "Shadows" -> shadows = parseSignFloat(text)
                                    "Whites2012", "Whites" -> whites = parseSignFloat(text)
                                    "Blacks2012", "Blacks" -> blacks = parseSignFloat(text)
                                    "Temperature" -> temperature = parseSignFloat(text)
                                    "Tint" -> tint = parseSignFloat(text)
                                    "Vibrance" -> vibrance = parseSignFloat(text)
                                    "Saturation" -> saturation = parseSignFloat(text)
                                    "Clarity2012", "Clarity" -> clarity = parseSignFloat(text)
                                    "Dehaze" -> dehaze = parseSignFloat(text)
                                    "PostCropVignetteAmount" -> vignette = parseSignFloat(text)
                                    "GrainAmount" -> grain = parseSignFloat(text)
                                    "SplitToningShadowHue", "ColorGradeShadowHue" -> shadowHue = parseSignFloat(text)
                                    "SplitToningShadowSaturation", "ColorGradeShadowSat" -> shadowSat = parseSignFloat(text)
                                    "SplitToningHighlightHue", "ColorGradeHighlightHue" -> highlightHue = parseSignFloat(text)
                                    "SplitToningHighlightSaturation", "ColorGradeHighlightSat" -> highlightSat = parseSignFloat(text)
                                    else -> {
                                        if (currentTag in KNOWN_UNSUPPORTED) {
                                            unsupportedFound.add(currentTag)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name.contains("ToneCurve", ignoreCase = true)) {
                            insideToneCurve = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Fallback regex extraction if XML pull parser fails or on desktop JVM
            return fallbackRegexExtraction(xmlContent, fallbackName)
        }

        return XmpPreset(
            name = presetName,
            rawXml = xmlContent,
            exposure = exposure.coerceIn(-5f, 5f),
            contrast = contrast.coerceIn(-100f, 100f),
            highlights = highlights.coerceIn(-100f, 100f),
            shadows = shadows.coerceIn(-100f, 100f),
            whites = whites.coerceIn(-100f, 100f),
            blacks = blacks.coerceIn(-100f, 100f),
            temperature = temperature.coerceIn(-100f, 100f),
            tint = tint.coerceIn(-100f, 100f),
            vibrance = vibrance.coerceIn(-100f, 100f),
            saturation = saturation.coerceIn(-100f, 100f),
            clarity = clarity.coerceIn(-100f, 100f),
            dehaze = dehaze.coerceIn(-100f, 100f),
            vignette = vignette.coerceIn(-100f, 100f),
            grain = grain.coerceIn(0f, 100f),
            shadowHue = shadowHue.coerceIn(0f, 360f),
            shadowSat = shadowSat.coerceIn(0f, 100f),
            highlightHue = highlightHue.coerceIn(0f, 360f),
            highlightSat = highlightSat.coerceIn(0f, 100f),
            balance = balance.coerceIn(-100f, 100f),
            toneCurvePoints = toneCurvePoints,
            unsupportedTags = unsupportedFound.toList().sorted()
        )
    }

    private fun parseSignFloat(str: String): Float {
        return try {
            val cleaned = str.replace("+", "").trim()
            cleaned.toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    private fun parseToneCurvePoint(text: String): Pair<Float, Float>? {
        val parts = text.split(",").map { it.trim() }
        if (parts.size >= 2) {
            val x = parts[0].toFloatOrNull() ?: return null
            val y = parts[1].toFloatOrNull() ?: return null
            // Points can be 0..255 or 0..1
            val normX = if (x > 1f) (x / 255f).coerceIn(0f, 1f) else x.coerceIn(0f, 1f)
            val normY = if (y > 1f) (y / 255f).coerceIn(0f, 1f) else y.coerceIn(0f, 1f)
            return Pair(normX, normY)
        }
        return null
    }

    private fun fallbackRegexExtraction(content: String, fallbackName: String): XmpPreset {
        fun extractString(attr: String): String? {
            val regexAttr = Regex("""(?:crs:)?$attr="([^"]+)"""", RegexOption.IGNORE_CASE)
            regexAttr.find(content)?.groupValues?.get(1)?.let { return it.trim() }
            val regexTag = Regex("""<(?:crs:)?$attr>([^<]+)</(?:crs:)?$attr>""", RegexOption.IGNORE_CASE)
            return regexTag.find(content)?.groupValues?.get(1)?.trim()
        }

        fun extractFloat(attr: String): Float {
            val str = extractString(attr) ?: return 0f
            return parseSignFloat(str)
        }

        val unsupported = mutableSetOf<String>()
        for (tag in KNOWN_UNSUPPORTED) {
            if (content.contains(tag, ignoreCase = true)) {
                unsupported.add(tag)
            }
        }

        val name = extractString("PresetName") ?: extractString("presetName") ?: fallbackName

        return XmpPreset(
            name = name,
            rawXml = content,
            exposure = extractFloat("Exposure2012").ifZero { extractFloat("Exposure") },
            contrast = extractFloat("Contrast2012").ifZero { extractFloat("Contrast") },
            highlights = extractFloat("Highlights2012").ifZero { extractFloat("Highlights") },
            shadows = extractFloat("Shadows2012").ifZero { extractFloat("Shadows") },
            whites = extractFloat("Whites2012").ifZero { extractFloat("Whites") },
            blacks = extractFloat("Blacks2012").ifZero { extractFloat("Blacks") },
            temperature = extractFloat("Temperature"),
            tint = extractFloat("Tint"),
            vibrance = extractFloat("Vibrance"),
            saturation = extractFloat("Saturation"),
            clarity = extractFloat("Clarity2012").ifZero { extractFloat("Clarity") },
            dehaze = extractFloat("Dehaze"),
            vignette = extractFloat("PostCropVignetteAmount"),
            grain = extractFloat("GrainAmount"),
            shadowHue = extractFloat("SplitToningShadowHue").ifZero { extractFloat("ColorGradeShadowHue") },
            shadowSat = extractFloat("SplitToningShadowSaturation").ifZero { extractFloat("ColorGradeShadowSat") },
            highlightHue = extractFloat("SplitToningHighlightHue").ifZero { extractFloat("ColorGradeHighlightHue") },
            highlightSat = extractFloat("SplitToningHighlightSaturation").ifZero { extractFloat("ColorGradeHighlightSat") },
            unsupportedTags = unsupported.toList().sorted()
        )
    }

    private fun Float.ifZero(other: () -> Float): Float = if (this == 0f) other() else this

    /**
     * Serializes an XmpPreset back into an Adobe-compatible XMP XML string.
     */
    fun toXml(preset: XmpPreset): String {
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 7.0-c000">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                xmlns:crs="http://ns.adobe.com/camera-raw-settings/1.0/"
                crs:PresetType="Normal"
                crs:PresetName="${preset.name}"
                crs:Exposure2012="${if (preset.exposure >= 0) "+" else ""}${String.format("%.2f", preset.exposure)}"
                crs:Contrast2012="${preset.contrast.toInt()}"
                crs:Highlights2012="${preset.highlights.toInt()}"
                crs:Shadows2012="${preset.shadows.toInt()}"
                crs:Whites2012="${preset.whites.toInt()}"
                crs:Blacks2012="${preset.blacks.toInt()}"
                crs:Temperature="${preset.temperature.toInt()}"
                crs:Tint="${preset.tint.toInt()}"
                crs:Vibrance="${preset.vibrance.toInt()}"
                crs:Saturation="${preset.saturation.toInt()}"
                crs:Clarity2012="${preset.clarity.toInt()}"
                crs:Dehaze="${preset.dehaze.toInt()}"
                crs:PostCropVignetteAmount="${preset.vignette.toInt()}"
                crs:GrainAmount="${preset.grain.toInt()}"
                crs:SplitToningShadowHue="${preset.shadowHue.toInt()}"
                crs:SplitToningShadowSaturation="${preset.shadowSat.toInt()}"
                crs:SplitToningHighlightHue="${preset.highlightHue.toInt()}"
                crs:SplitToningHighlightSaturation="${preset.highlightSat.toInt()}"
                crs:SplitToningBalance="${preset.balance.toInt()}" />
             </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }
}
