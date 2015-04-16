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
     * Open the IME
     */
    void openIME();
}
