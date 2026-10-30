package com.example

import com.example.xmp.XmpParser
import com.example.xmp.XmpPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testXmpPresetParsing() {
    val sampleXmp = """
      <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 7.0-c000">
       <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about=""
          xmlns:crs="http://ns.adobe.com/camera-raw-settings/1.0/"
          crs:PresetName="Cinematic Teal"
          crs:Exposure2012="+0.35"
          crs:Contrast2012="+25"
          crs:Highlights2012="-20"
          crs:Shadows2012="+15"
          crs:Temperature="-8"
          crs:Tint="+4"
          crs:Vibrance="+30"
          crs:Saturation="-10"
          crs:LensProfileEnable="1">
        </rdf:Description>
       </rdf:RDF>
      </x:xmpmeta>
    """.trimIndent()

    val parsed = XmpParser.parse(sampleXmp, fallbackName = "Fallback")
    assertEquals("Cinematic Teal", parsed.name)
    assertEquals(0.35f, parsed.exposure, 0.01f)
    assertEquals(25f, parsed.contrast, 0.01f)
    assertEquals(-20f, parsed.highlights, 0.01f)
    assertEquals(15f, parsed.shadows, 0.01f)
    assertEquals(-8f, parsed.temperature, 0.01f)
    assertEquals(4f, parsed.tint, 0.01f)
    assertEquals(30f, parsed.vibrance, 0.01f)
    assertEquals(-10f, parsed.saturation, 0.01f)
    assertTrue(parsed.unsupportedTags.contains("LensProfileEnable"))
  }

  @Test
  fun testXmpSerializationAndExport() {
    val preset = XmpPreset(
      name = "Vintage Kodak",
      exposure = 0.2f,
      contrast = 15f,
      highlights = -10f,
      shadows = 20f,
      temperature = 12f
    )
    val xml = XmpParser.toXml(preset)
    assertTrue(xml.contains("Vintage Kodak"))
    assertTrue(xml.contains("crs:Exposure2012=\"+0.20\""))

    val reparsed = XmpParser.parse(xml)
    assertEquals("Vintage Kodak", reparsed.name)
    assertEquals(0.2f, reparsed.exposure, 0.01f)
    assertEquals(15f, reparsed.contrast, 0.01f)
  }
}
