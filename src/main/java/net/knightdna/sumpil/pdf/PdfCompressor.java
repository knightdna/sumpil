package net.knightdna.sumpil.pdf;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.filetypedetector.FileType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class PdfCompressor {

    // By default, the quality of the compressed image will be 50% of the original image
    private static final float COMPRESSION_FACTOR = 0.5f;

    @SneakyThrows
    public void compress(Path originalPdfPath) {
        Path parentDirectory = originalPdfPath.getParent();
        String pdfBaseName = originalPdfPath.getFileName().toString();
        String fileName = com.google.common.io.Files.getNameWithoutExtension(pdfBaseName);
        String fileExtension = com.google.common.io.Files.getFileExtension(pdfBaseName);

        Path compressedPdfPath = parentDirectory.resolve(String.format("%s-compressed.%s", fileName, fileExtension));

        RandomAccessRead randomAccessRead = new RandomAccessBufferedFileInputStream(Files.newInputStream(originalPdfPath));
        PDFParser parser = new PDFParser(randomAccessRead);
        parser.parse();

        @Cleanup PDDocument document = parser.getPDDocument();
        document.getPages()
                .iterator()
                .forEachRemaining(page -> {
                    // For each page in the PDF document:
                    // 1. Get its image resource and perform image compression
                    // 2. Override (replace) the original image with the compressed one
                    scanResources(page.getResources(), document);
                });

        @Cleanup OutputStream compressedPdfOutputStream = Files.newOutputStream(compressedPdfPath);
        document.save(compressedPdfOutputStream);
    }

    @SneakyThrows
    private void scanResources(PDResources resources, PDDocument document) {
        Iterable<COSName> xObjectNames = resources.getXObjectNames();
        for (COSName name : xObjectNames) {
            PDXObject xObject = resources.getXObject(name);

            if (xObject instanceof PDFormXObject) {
                // Form XObject is similar to a mini-PDF embedded inside the main PDF document;
                // thus, we need recursively scan its pages and compress its image resources
                scanResources(((PDFormXObject) xObject).getResources(), document);
            }

            if (!(xObject instanceof PDImageXObject)) {
                // We will not process the non-image resource of the PDF page
                continue;
            }

            PDImageXObject image = (PDImageXObject) xObject;

            // Prepare the image (JPEG) writer
            ImageWriter jpegWriter = ImageIO
                    .getImageWritersByFormatName(FileType.JPEG.name().toLowerCase())
                    .next();

            // Prepare the image compression settings
            ImageWriteParam compressionParams = jpegWriter.getDefaultWriteParam();
            compressionParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            compressionParams.setCompressionQuality(COMPRESSION_FACTOR);

            // Prepare the output buffer of the image
            ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
            jpegWriter.setOutput(ImageIO.createImageOutputStream(imageOutputStream));
            // Write the compressed image (according to image compression parameters) to the output buffer
            jpegWriter.write(null, new IIOImage(image.getImage(), null, null), compressionParams);

            // Replace the current PDF image with the compressed one
            PDImageXObject compressedImage = PDImageXObject.createFromByteArray(
                    // current PDF document
                    document,
                    // compressed image byte array
                    imageOutputStream.toByteArray(),
                    "compressedImage");
            resources.put(name, compressedImage);
        }
    }

}
