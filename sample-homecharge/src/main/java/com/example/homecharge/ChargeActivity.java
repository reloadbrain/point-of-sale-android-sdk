/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.homecharge;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.squareup.sdk.pos.TransactionRequest;
import com.squareup.sdk.pos.PosClient;
import com.squareup.sdk.pos.PosSdk;

import static com.squareup.sdk.pos.TransactionRequest.TenderType.CARD_FROM_READER;

public class ChargeActivity extends AppCompatActivity {

  private static final int CHARGE_REQUEST_CODE = 1;

  private PosClient posClient;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_charge);
    posClient = PosSdk.createClient(this, BuildConfig.SQUARE_CLIENT_ID);
  }

  public void startTransaction(int dollarAmount, String note) {
    TransactionRequest request = new TransactionRequest.Builder(dollarAmount * 1_00).note(note)
        .autoReturn(true)
        .restrictTendersTo(CARD_FROM_READER)
        .build();
    try {
      Intent intent = posClient.createTransactionIntent(request);
      startActivityForResult(intent, CHARGE_REQUEST_CODE);
    } catch (ActivityNotFoundException e) {
      showDialog("Error", "Square POS is not installed", null);
      posClient.openPointOfSalePlayStoreListing();
    }
  }

  private void showDialog(String title, String message, DialogInterface.OnClickListener listener) {
    Log.d("ChargeActivity", title + " " + message);
    new AlertDialog.Builder(this).setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, listener)
        .show();
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CHARGE_REQUEST_CODE) {
      if (data == null) {
        showDialog("Error", "Square POS was uninstalled or crashed", null);
        return;
      }

      if (resultCode == Activity.RESULT_OK) {
        TransactionRequest.Success success = posClient.parseTransactionSuccess(data);
        String message = "Client transaction id: " + success.transaction.clientId();
        Toast.makeText(this, "Success, " + message, Toast.LENGTH_LONG).show();
      } else {
        TransactionRequest.Error error = posClient.parseTransactionError(data);
        showDialog("Error: " + error.code, error.debugDescription, null);
      }
    }
  }
}
