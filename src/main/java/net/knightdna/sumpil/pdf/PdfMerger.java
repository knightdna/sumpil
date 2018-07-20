package net.knightdna.sumpil.pdf;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@UtilityClass
public class PdfMerger {

    @SneakyThrows
    public void merge(Path destinationFile, Path... sourceFiles) {
        @Cleanup OutputStream destinationFileOutputStream = Files.newOutputStream(destinationFile);

        PDFMergerUtility mergerUtility = new PDFMergerUtility();
        mergerUtility.setDestinationStream(destinationFileOutputStream);
        for (Path file : Arrays.asList(sourceFiles)) {
            mergerUtility.addSource(file.toFile());
        }

        mergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
    }

}
