package com.sirot.positivitydose;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by sirot on 9/9/2017.
 */

public class PositiveDoseFragment extends Fragment {

    private Button mSelectFriend;
    private Button mSendButton;
    private TextView mLastPositivityDoseSent;
    private Friend mFriend;

    private static final int REQUEST_CONTACT = 0;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1; // Requesting permission to access one contact
    private static final int PERMISSIONS_SEND_SMS = 2;

    private static final String SAVED_STATE_FRIEND_NAME = "SAVED_STATE_FRIEND_NAME";
    private static final String SAVED_STATE_FRIEND_PHONE_NUMBER = "SAVED_STATE_FRIEND_PHONE_NUMBER";

    final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_positive_dose, container, false);

        if (mFriend == null) {
            mFriend = new Friend();
        }
        String savedContactName = null;
        if (savedInstanceState != null) {
            savedContactName = savedInstanceState.getString(SAVED_STATE_FRIEND_NAME);
            String number = savedInstanceState.getString(SAVED_STATE_FRIEND_PHONE_NUMBER);
            mFriend.setFriendName(savedContactName);
            mFriend.setPhoneNumber(number);
        }

        mSelectFriend = (Button) v.findViewById(R.id.select_friend);
        if (savedContactName != null) {
            mSelectFriend.setText(savedContactName);
        }
        mSendButton = (Button) v.findViewById(R.id.send_positivity_dose);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_SEND_SMS);
//                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                    if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_DENIED) {

                        //Log.d("permission", "permission denied to SEND_SMS - requesting it");
                        String[] permissions = {Manifest.permission.SEND_SMS};

                        requestPermissions(permissions, PERMISSIONS_SEND_SMS);

                    }
                }
                if (mFriend.getPhoneNumber() != null) {
                    sendPositivityDose();
                } else {
                    sendCustomizedPositivityDoseWithoutPhoneNumber();
                }
            }
        });

        mLastPositivityDoseSent = (TextView) v.findViewById(R.id.last_positivity_dose_sent);

        mSelectFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
                }

                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        return v;
        }//end oncreateview

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            populateName(contactUri);
        }
    }

    /**
     *  Retrieves the contact name and phone number from Address Book
     *  @param contactUri
     *
     */
    private void populateName(Uri contactUri) {
        String[] queryFields = new String[]{
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts._ID
        };
        // Cursor stores the database information similar to the way tables are
        Cursor cursor = getActivity().getContentResolver()
                .query(contactUri, queryFields, null, null, null);
        try {
            if (cursor.getCount() == 0) {
                return;
            }
            cursor.moveToFirst();
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            String name = cursor.getString(nameColumnIndex);
            mFriend.setFriendName(name);
            mSelectFriend.setText(name);
            if (name != null) {
                String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                populatePhoneNumber(hasPhone.equalsIgnoreCase("1"), id);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }//end populateName()



    /**
     *  Retrieves the phone number from Address Book
     *  @param hasPhone true if phone number is found, false otherwise
     *  @param id id of the contact   **/
    private void populatePhoneNumber(boolean hasPhone, String id) {
        String phoneNumber = null;
        if (hasPhone) {
            Cursor phones = getActivity().getContentResolver()
                    .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
            try {
                phones.moveToFirst();
                phoneNumber = phones.getString(phones.getColumnIndex("data1"));
            } finally {
                if (phones != null) {
                    phones.close();
                }
            }
        }
        mFriend.setPhoneNumber(phoneNumber);
    }//end populatePhoneNumber



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mFriend != null) {
            outState.putString(SAVED_STATE_FRIEND_NAME, mFriend.getFriendName());
            outState.putString(SAVED_STATE_FRIEND_PHONE_NUMBER, mFriend.getPhoneNumber());
        }
    }//end onSaveInstanceState


    /**
     *  Method responsible for sending positivity dose messages
     *  @return true if message sent successfully, false otherwise   **/
    private void sendPositivityDose() {
        SmsManager smsManager;
        try {
            smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(mFriend.getPhoneNumber(), null, getString(R.string.message), null, null);
            if (!((Activity) getContext()).isFinishing()) {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.sent),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mSelectFriend.setText(getString(R.string.select_friend));
        }
    }//end sendPositivityDose



    /**
     * Method responsible for sending messages without phone number  */
    private void sendCustomizedPositivityDoseWithoutPhoneNumber() {
        Toast.makeText(getActivity().getApplicationContext(), R.string.noPhoneNumber, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.message));
        startActivity(i);
    }//end sendCustomizedPositivityDoseWithoutPhoneNumber



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_SEND_SMS:
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendPositivityDose();
                } else {

                }
                break;
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Addressed by onActivity on result. Can update 3.1.1
                }
                break;
        }
    }//end onRequestPermissionsResult





}//end class



