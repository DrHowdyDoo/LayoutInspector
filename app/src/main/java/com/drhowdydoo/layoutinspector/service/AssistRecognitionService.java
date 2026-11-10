package com.drhowdydoo.layoutinspector.service;

import android.content.Intent;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class AssistRecognitionService extends RecognitionService {

    @Override
    protected void onStartListening(
            Intent recognizerIntent,
            Callback listener) {
        try {
            listener.error(SpeechRecognizer.ERROR_CLIENT);
        } catch (RemoteException e) {
            Log.e("AssistRecognition", "Failed to report recognition error", e);
        }
    }

    @Override
    protected void onStopListening(Callback listener) {
    }

    @Override
    protected void onCancel(Callback listener) {
    }
}
