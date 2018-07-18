package net.knightdna.sumpil.pdf;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PdfSplitter {

    @SneakyThrows
    public static void split(Path pdfFilePath, int numberOfFiles) {
        Splitter splitter = new Splitter();

        @Cleanup InputStream inputStream = Files.newInputStream(pdfFilePath);
        @Cleanup PDDocument allPdfDocuments = PDDocument.load(inputStream);

        int numberOfAllPages = allPdfDocuments.getNumberOfPages();
        int numberOfPagesPerFile = (int) Math.ceil(numberOfAllPages / (float) numberOfFiles);

        // This will split the PDF into multiple files,
        // where each file having total page number equals to numberOfPagesPerFile
        //
        // Default value is 1
        splitter.setSplitAtPage(numberOfPagesPerFile);

        List<PDDocument> pdfDocuments = splitter.split(allPdfDocuments);

        Path parentDirectory = pdfFilePath.getParent();
        String pdfBaseName = pdfFilePath.getFileName().toString();
        String fileName = com.google.common.io.Files.getNameWithoutExtension(pdfBaseName);
        String fileExtension = com.google.common.io.Files.getFileExtension(pdfBaseName);

        // Split file name suffix starts with 1
        int i = 1;
        for (PDDocument document : pdfDocuments) {
            Path generatedFilePath = parentDirectory.resolve(String.format("%s-%s.%s", fileName, i, fileExtension));
            @Cleanup OutputStream outputStream = Files.newOutputStream(generatedFilePath);
            document.save(outputStream);
            i++;
        }
    }

}
