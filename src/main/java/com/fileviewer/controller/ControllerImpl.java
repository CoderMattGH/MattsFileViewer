package com.fileviewer.controller;

import com.fileviewer.dataprocessing.DataViewer;
import com.fileviewer.dataprocessing.FileLoader;
import com.fileviewer.gui.GUI;
import com.fileviewer.gui.progressbar.ProgressBar;
import com.fileviewer.gui.progressbar.ProgressBarFactory;
import com.fileviewer.model.Model;
import com.fileviewer.observer.ProgObserver;
import com.fileviewer.observer.ProgObserverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;

import static com.fileviewer.dataprocessing.DataViewer.DataType;

public class ControllerImpl implements Controller {
    private static final Logger logger = LogManager.getLogger(ControllerImpl.class);

    private final ProgObserverFactory progObserverFactory;
    private final ProgressBarFactory progressBarFactory;
    private final FileLoader fileLoader;
    private final Model model;
    private final DataViewer dataViewer;

    private GUI gui = null;

    public ControllerImpl(FileLoader fileLoader, Model model, DataViewer dataViewer,
            ProgObserverFactory progObserverFactory, ProgressBarFactory progressBarFactory) {
        logger.debug("Constructing ControllerImpl");

        this.fileLoader = fileLoader;
        this.model = model;
        this.dataViewer = dataViewer;
        this.progObserverFactory = progObserverFactory;
        this.progressBarFactory = progressBarFactory;
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    // THREADED?
    public void loadFile() {
        final JFileChooser fileChooser = new JFileChooser();

        int returnVal = fileChooser.showOpenDialog(gui);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ProgObserver observer = progObserverFactory.getInstance();
            showProgressBar(observer);

            File file = fileChooser.getSelectedFile();

            int[] tempFileData = fileLoader.loadFile(file, observer);

            observer.setPercentage(0);

            // If an error occurred or file was null then return.
            if (tempFileData == null) {
                gui.displayError("File size was too large.");
                observer.setIsFinished(true);

                return;
            }

            model.setLastFileLoadedData(tempFileData);
            model.setStartByteIndex(0);

            gui.setPageLabel(model.getCurrentPage());
            gui.setFileSizeLabel(model.getLastFileLoadedData().length);
            gui.setFileNameLabel(file.getName());

            dataViewer.displayData(model.getLastFileLoadedData(), observer, model.getCurrentType(),
                    model.getStartByteIndex(),
                    model.getStartByteIndex() + model.getMaxBytesPerPage());
            observer.setIsFinished(true);
        }
    }

    public void displayData(DataType type) {
        ProgObserver observer = progObserverFactory.getInstance();
        showProgressBar(observer);

        dataViewer.displayData(model.getLastFileLoadedData(), observer, model.getCurrentType(),
                model.getStartByteIndex(),
                model.getStartByteIndex() + model.getMaxBytesPerPage());

        observer.setIsFinished(true);
    }

    public void showProgressBar(ProgObserver observer) {
        Thread thread = new Thread(() -> {
                ProgressBar progressBar = progressBarFactory.getInstance(gui, observer);

                while(!observer.isFinished()) {
                    progressBar.setPercentage(observer.getPercentage());

                    try {
                        Thread.sleep(20);
                    } catch (Exception ignored) {}
                }

                logger.debug("Trying to destroy ProgressBar...");
                progressBar.destroyProgressBar();
        });
        thread.start();
    }

    // Threaded?
    public void changeViewType(DataType type) {
        model.setStartByteIndex(0);
        gui.setPageLabel(model.getCurrentPage());

        model.setCurrentType(type);
        displayData(model.getCurrentType());
    }

    public void showNextPage() {
        logger.debug("Fetching next page.");

        if (model.getLastFileLoadedData() == null)
            return;

        int tempStartIndex = model.getStartByteIndex() + model.getMaxBytesPerPage();
        if (tempStartIndex >= model.getLastFileLoadedData().length) {
            gui.displayMessage("No more data.");

            return;
        }

        model.setStartByteIndex(tempStartIndex);
        gui.setPageLabel(model.getCurrentPage());

        displayData(model.getCurrentType());
    }

    public void showPrevPage() {
        logger.debug("Fetching previous page.");

        int result = model.getStartByteIndex() - model.getMaxBytesPerPage();

        if (result < 0)
            model.setStartByteIndex(0);
        else
            model.setStartByteIndex(result);

        gui.setPageLabel(model.getCurrentPage());

        displayData(model.getCurrentType());
    }

    public void showFirstPage() {
        logger.debug("Fetching first page.");

        model.setStartByteIndex(0);

        displayData(model.getCurrentType());
    }

    public void resetTextOutput() {
        gui.resetTextOutput();
    }

    public void appendTextOutput(String string) {
        gui.appendTextOutput(string);
    }
}
