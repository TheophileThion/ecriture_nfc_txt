package com.theot.ecriture_nfc_txt;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    NfcAdapter mNfcAdapter;
    String serialId ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        TextView affichage = findViewById(R.id.affichage);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag montag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "Hi there ! " , Toast.LENGTH_SHORT).show();
            affichage.setText("UID :" + inverser(toHexString(montag.getId())));
           // affichage.setText(toHexString(montag.getId()));
            File mFile = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.example/files/contenu_tag.txt");
            //Ecriture d'un txt avec le contenu du tag
            NfcV tagV = NfcV.get(montag);


            int offset = 9;  // offset of first block to read
            int blocks = 1;  // number of blocks to read
            byte[] cmad = new byte[]{
                    (byte)0x60,                  // flags: addressed (= UID field present)
                    (byte)0x23,                  // command: READ MULTIPLE BLOCKS
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,  // placeholder for tag UID
                    (byte)(offset & 0x0ff),      // first block number
                    (byte)((blocks - 1) & 0x0ff) // number of blocks (-1 as 0x00 means one block)
            };

            System.arraycopy(montag.getId(), 0, cmad, 2, 8);

            byte[] erreurtest = null;
            byte[] response = null;
            byte[] responseb = null;
            byte[] adc2 = new byte[]{(byte)0x00, (byte)0x00};
            byte[] adc1 = new byte[]{(byte)0x00, (byte)0x00};
            TextView donnee = findViewById(R.id.donneesNfc);
            TextView temperature = findViewById(R.id.temperature);
            TextView mesurefinal = findViewById(R.id.mesurefinal);
            float temperatureext = 0;
            try {
                tagV.connect();

                byte[] cmd = new byte[] {(byte)0x02,(byte) 0x21,(byte) 0x02,(byte) 0x19,(byte) 0x19,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00};
                erreurtest=tagV.transceive(cmd); //Bloc 2 initialisé

                cmd = new byte[]{(byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40};
                erreurtest=tagV.transceive(cmd);   //Bloc 0 initialisé

                response = tagV.transceive(cmad);   // read multiple block sur block 9 pour l'ADC2
                System.arraycopy(response,2,adc2,0,2); //Extraction de ADC2 du block 9 => ADC2 set !
                System.arraycopy(response,4,adc1,0,2);
                int test = byteToInt(adc2,2);

                tagV.close();
                tagV.connect();

/**
                cmd = new byte[] {(byte)0x02,(byte) 0x21,(byte) 0x02,(byte) 0x2C,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00};
                erreurtest=tagV.transceive(cmd); //Bloc 2 initialisé

                cmd = new byte[]{(byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                erreurtest=tagV.transceive(cmd);   //Bloc 0 initialisé

                responseb = tagV.transceive(cmad);   // read multiple block sur block 9 pour l'ADC1
                System.arraycopy(responseb,4,adc1,0,2); //Extraction de ADC2 du block 9 => ADC1 set !
*/
                int test1 = byteToInt(adc1,2);
                tagV.close();


                erreurtest[0] = tagV.getResponseFlags();


                double possibleres =  (((float)test/(float)test1)*100000);

                //donnee.setText( toHexString(response) + "  " + toHexString(responseb));
                donnee.setText( toHexString(response));
                temperature.setText("ADC2 : "+inverser(toHexString(adc2)) + ", ADC1 : " + inverser(toHexString(adc1)) + ", test : " + test + ", test1 : " + test1);
               // temperature.setText("ADC2 : "+inverser(toHexString(adc2)));


                float voltage = (float) (( (float) test/16383)*0.9);
                float current =  (float) (2.4 * Math.pow(10,-6));
                double resistance = voltage/current;
                double mesure =  ((2.45-(resistance/100000))/0.057);


                double mesure2 =  ((2.45-(possibleres/100000))/0.057);
                temperatureext= (float) mesure2;

                mesurefinal.setText("Température = " + mesure +", Température2 :" + mesure2);


            } catch (IOException e) {
                e.printStackTrace();
            }


            try {

                mFile.createNewFile();
                FileOutputStream output = new FileOutputStream(mFile, true);
                Date date = new Date();
                String chaine = date.toString() + " Tag détecté : " + inverser(toHexString(montag.getId())) + " et il fait " + temperatureext +"°";
                output.write((chaine + "\n").getBytes());
               // Toast.makeText(MainActivity.this, "On a ecrit ", Toast.LENGTH_LONG).show();
                if (output != null)
                    output.close();
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String inverser (String chaine) {
        String retour = "";
        for (int i=0; i < chaine.length()/2; i++) {
            retour=chaine.substring(i*2,i*2+2) + retour ;
        }
        return  retour;
    }

    public static String separer (String chaine) {
        String retour = "";
        int compte = 0;
        while (compte<chaine.length()){
            retour = retour + chaine.substring(compte,compte+1);
            compte++;
            if (compte%20==0){
                retour=retour+"\n";
            }
        }
    return retour;
    }

    public int byteToInt(byte[] bytes, int length) {
        int val = 0;
        if(length>4) throw new RuntimeException("Too big to fit in int");
        for (int i = 1; i < length + 1 ; i++) {
            val=val<<8;
            val=val|(bytes[length-i] & 0xFF);      //a l'envers
           // val=val|(bytes[i-1] & 0xFF);     //a l'endroit
        }
        return val;
    }




    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v / 16];
            hexChars[j * 2 + 1] = hexArray[v % 16];
        }
        return new String(hexChars);
    }


    @Override
    public void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }
    @Override
    public void onPause() {
        super.onPause();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }
}