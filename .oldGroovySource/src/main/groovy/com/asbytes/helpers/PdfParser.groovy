package com.asbytes.helpers


import org.apache.pdfbox.io.RandomAccessBuffer
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

public class PdfParser {
    private byte[] bytes

    public static void main(String[] args) {
        if (args.length == 0) {
            println "No args given, expected directory path containing PDF's"
            return
        }

        def source = new File(args.first())
        println "Parsing path: ${source.getAbsolutePath()}"

        if (!source.isDirectory()) {
            println "Given path is not a directory"
            return
        }

        def pdfs = source.listFiles().findAll {it.name.endsWith('pdf') }
        pdfs.each {pdf ->
            new File("${pdf.getAbsolutePath()}.txt").text = new PdfParser(pdf).getPdfText()
            println "created ${pdf.getAbsolutePath()}.txt"
        }
    }

    public PdfParser(byte[] bytes) {
        Exceptions.throwOnFalse(bytes != null, 'Could not find PDF data.')
        this.bytes = bytes
    }

    public PdfParser(File pdfFile) {
        this(pdfFile.bytes)
    }

    public String getPdfText() {
        return getPdfText(bytes)
    }

    public static String getPdfText(byte[] bytes) {
        String pdfContent = null
        new RandomAccessBuffer(bytes).withCloseable { buffer ->
            PDFParser parser = new PDFParser(buffer)
            parser.parse()
            new PDDocument(parser.getDocument()).withCloseable { pdDoc ->
                PDFTextStripper pdfStripper = new PDFTextStripper()
                pdfContent = pdfStripper.getText(pdDoc)
            }
        }
        return pdfContent
    }
}
