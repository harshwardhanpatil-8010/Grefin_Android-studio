// MainActivity.java
package com.example.grefin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> smsList = new ArrayList<>();
    private ListView listView;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://192.168.116.12:8000")  // Ensure this URL is correct
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private TransactionApi transactionApi = retrofit.create(TransactionApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            readSms();
        }
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                if (body.toLowerCase().contains("amt sent") ||
                        body.toLowerCase().contains("debited") ||
                        body.toLowerCase().contains("credited")) {
                    String transactionAmount = extractAmount(body);
                    smsList.add("Sender: " + address + "\nMessage: " + body + "\nAmount: " + transactionAmount);
                    System.out.println(address+"  "+body);
                    sendTransactionToServer(address, transactionAmount);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private String extractAmount(String message) {
        String amountPattern = "\\b\\d+(\\.\\d{1,2})?\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(amountPattern);
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group();
        }
        return "Amount not found";
    }

    private void sendTransactionToServer(String receiverName, String amountPaid) {
        Transaction transaction = new Transaction(receiverName, amountPaid);
        Log.d("TransactionApi", "Sending transaction: " + transaction.toString());

        Call<Void> call = transactionApi.sendTransaction(transaction);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("TransactionApi", "Transaction sent successfully");
                } else {
                    Log.e("TransactionApi", "Failed to send transaction: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("TransactionApi", "Error: " + t.getMessage());
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms();
            } else {
                Log.e("SMS", "Permission denied to read SMS");
            }
        }
    }
}

