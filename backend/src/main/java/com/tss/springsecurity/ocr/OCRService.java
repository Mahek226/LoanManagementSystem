package com.tss.springsecurity.ocr;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
public class OCRService {

    @Value("${ocr.tesseract.datapath}")
    private String tessDataPath;  // Should point to tessdata folder directly

    @Value("${ocr.tesseract.lang:eng}")
    private String tessLang;

    @Value("${ocr.tesseract.executable:tesseract}")
    private String tesseractExecutable;

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ?
                file.getOriginalFilename().toLowerCase(Locale.ROOT) : "upload";

        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else {
            return extractFromImage(file);
        }
    }

    private String extractFromImage(MultipartFile file) throws IOException {
        Path tempImage = Files.createTempFile("ocr_img_", getExtension(file.getOriginalFilename()));
        Files.copy(file.getInputStream(), tempImage, StandardCopyOption.REPLACE_EXISTING);

        try {
            return runTesseract(tempImage);
        } finally {
            Files.deleteIfExists(tempImage);
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        Path tempPdf = Files.createTempFile("ocr_pdf_", ".pdf");
        Files.copy(file.getInputStream(), tempPdf, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument doc = Loader.loadPDF(tempPdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                Path tempPageImage = Files.createTempFile("ocr_page_" + i + "_", ".png");
                try {
                    ImageIO.write(pageImage, "PNG", tempPageImage.toFile());
                    String pageText = runTesseract(tempPageImage);
                    sb.append(pageText).append("\n");
                } finally {
                    Files.deleteIfExists(tempPageImage);
                }
            }
            return sb.toString();
        } finally {
            Files.deleteIfExists(tempPdf);
        }
    }

    private String runTesseract(Path imagePath) throws IOException {
        Path tessDataFolder = Path.of(tessDataPath);
        if (!Files.exists(tessDataFolder)) {
            throw new IOException("tessdata folder not found at: " + tessDataFolder.toAbsolutePath());
        }

        Path langFile = tessDataFolder.resolve(tessLang + ".traineddata");
        if (!Files.exists(langFile)) {
            throw new IOException("Language file not found: " + langFile.toAbsolutePath());
        }

        Path outputBase = Files.createTempFile("ocr_out_", "");
        Path outputFile = Path.of(outputBase.toString() + ".txt");

        try {
            // ✅ Fix: use tessdata folder directly, not parent
            String tessdataDir = tessDataFolder.toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractExecutable,
                    imagePath.toAbsolutePath().toString(),
                    outputBase.toAbsolutePath().toString(),
                    "-l", tessLang,
                    "--tessdata-dir", tessdataDir
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Tesseract failed with exit code " + exitCode + ": " + output);
            }

            if (Files.exists(outputFile)) {
                return Files.readString(outputFile);
            } else {
                throw new IOException("Tesseract did not produce output file");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Tesseract process interrupted", e);
        } finally {
            Files.deleteIfExists(outputBase);
            Files.deleteIfExists(outputFile);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".tmp";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".tmp";
    }
}
