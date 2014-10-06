package com.dexafree.sample

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.InjectView
import com.arasthel.swissknife.annotations.OnBackground
import com.arasthel.swissknife.annotations.OnClick
import com.arasthel.swissknife.annotations.OnUIThread
import com.arasthel.swissknife.annotations.SaveInstance
import groovy.transform.CompileStatic

@CompileStatic
public class BackgroundActivity extends Activity {

    private Context mContext;

    @SaveInstance("MYSTRING")
    public String myString

    @SaveInstance
    public int myInt

    @SaveInstance
    public ArrayList<Person> myPersons


    @InjectView(R.id.edit_text)
    @SaveInstance
    public EditText textView;


    @InjectView(R.id.image_view) ImageView image

    @OnClick(R.id.load_button)
    public void onClick() {
        startLoading()
    }

    @OnBackground()
    public void startLoading() {
        String url = "https://camo.githubusercontent.com/216b3510229c2a6a77ccef60f258ac760a86fc79/687474703a2f2f692e696d6775722e636f6d2f4c53396f4859562e706e67";
        Bitmap imageBitmap;

        InputStream inputStream = new URL(url).openStream()
        imageBitmap = BitmapFactory.decodeStream(inputStream)

        setImage(imageBitmap)
    }

    @OnUIThread()
    public void setImage(Bitmap bitmap){
        image.setImageBitmap(bitmap)
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        mContext = this;
        setContentView(R.layout.activity_background)


        myString = "BEFORE"
        myPersons = new ArrayList<Person>()

        SwissKnife.inject(this)
        SwissKnife.restoreState(this, savedInstanceState)

        if(savedInstanceState == null){
            Log.d("SIS", "WAS NULL")

            myPersons.add(new Person("MyName", 55))
            myInt = 150
            myString = "I WILL BE SAVED"
        }

        Person p = myPersons.get(0)

        assert p.name == "MyName"
        assert p.age == 55

        assert myInt == 150

        assert myString == "I WILL BE SAVED"

    }


    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState)
    }




}