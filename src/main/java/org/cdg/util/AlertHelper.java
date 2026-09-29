// FICHEIRO: src/main/java/org/cdg/util/AlertHelper.java
package org.cdg.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class AlertHelper {

    public static void showError(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Erro");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }

    public static void showWarning(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensagem);
        alerta.setTitle("Atenção");
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    public static void showInformation(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensagem);
        alerta.setTitle("Informação");
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    public static boolean showConfirmation(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem);
        alerta.setTitle("Confirmação");
        alerta.setHeaderText(titulo);
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }
}