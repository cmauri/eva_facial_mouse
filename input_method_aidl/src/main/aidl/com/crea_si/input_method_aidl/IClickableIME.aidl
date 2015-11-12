package com.crea_si.input_method_aidl;

interface IClickableIME {
	/**
     * Try to click on an IME key
     * @param x - abscissa coordinate of the point (relative to the screen)
     * @param y - ordinate coordinate of the point (relative to the screen)
     * @return true if the point is within view bounds of the IME, false otherwise
     */
    boolean click (int x, int y);
    
    /**
     * Opens the IME
     */
    void openIME();
    
    /**
     * Closes the IME
     */
    void closeIME();

    /**
     * Open IME if closed, close otherwise
     */
     void toggleIME();
}
