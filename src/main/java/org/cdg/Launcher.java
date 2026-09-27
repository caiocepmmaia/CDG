package org.cdg;

public class Launcher {
    public static void main(String[] args) {
        // Correção de compatibilidade de teclado para JavaFX no Linux (Resolve o Shift/Caps Lock)
        System.setProperty("glass.accessible.force", "false");

        // O seu "truque" original para enganar e invocar o arranque do JavaFX
        org.cdg.App.main(args);
    }
}   