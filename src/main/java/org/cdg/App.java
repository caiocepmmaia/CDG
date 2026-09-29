package org.cdg;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Carrega o Dashboard FXML
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));

            // Cria a cena
            Scene scene = new Scene(root, 1000, 650);

            var cssResource = getClass().getResource("/view/style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }

            stage.setTitle("Gestão Financeira");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}