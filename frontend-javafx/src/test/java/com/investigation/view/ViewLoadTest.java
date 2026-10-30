package com.investigation.view;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ViewLoadTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testLoadAllViews() throws Exception {
        String[] views = {
                "/com/investigation/view/MainLayout.fxml",
                "/com/investigation/view/DashboardView.fxml",
                "/com/investigation/view/CasesView.fxml",
                "/com/investigation/view/EvidenceView.fxml",
                "/com/investigation/view/TimelineView.fxml",
                "/com/investigation/view/GraphView.fxml",
                "/com/investigation/view/AnomaliesView.fxml",
                "/com/investigation/view/AiSummaryView.fxml"
        };

        for (String viewPath : views) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Object root = loader.load();
            assertNotNull(root, "View should load: " + viewPath);
        }
    }
}
