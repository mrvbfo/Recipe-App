package com.mervebfo.recipebook

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.appsearch.SetSchemaRequest.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.mervebfo.recipebook.databinding.ActivityRecipeBinding
import java.io.ByteArrayOutputStream

class RecipeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeBinding
    private lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap:Bitmap?=null
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityRecipeBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        database=this.openOrCreateDatabase("Recipes", MODE_PRIVATE,null)
        registerLauncher()

        val intent=intent
        val info=intent.getStringExtra("info")
        if(info.equals("new")){
            binding.recipeName.setText("")
            binding.instruction.setText("")
            binding.ingredients.setText("")
            binding.btnSave.visibility=View.VISIBLE
            binding.imageView.setImageResource(R.drawable.img)
        }else{
            binding.btnSave.visibility=View.INVISIBLE
            val selectedId=intent.getIntExtra("id",1)

            val cursor=database.rawQuery("SELECT * FROM recipes Where id=?",arrayOf(selectedId.toString()))

            val recipeNameIx= cursor.getColumnIndex("recipename")
            val ingredientsIx=cursor.getColumnIndex("ingredients")
            val instructionsIx=cursor.getColumnIndex("instruction")
            val imageIx=cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.recipeName.setText(cursor.getString(recipeNameIx))
                binding.ingredients.setText(cursor.getString(ingredientsIx))
                binding.instruction.setText(cursor.getString(instructionsIx))

                val byteArray=cursor.getBlob(imageIx)
                val bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }
    }

    fun saveButton(view: View){

        val recipeName=binding.recipeName.text.toString()
        val ingredients=binding.ingredients.text.toString()
        val instruction=binding.instruction.text.toString()

        if(selectedBitmap!=null){
            val smallBitmap=makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream=ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray=outputStream.toByteArray()

            try {
            //    val database=this.openOrCreateDatabase("Recipes", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS recipes (id INTEGER PRIMARY KEY,recipename VARCHAR,ingredients VARCHAR,instruction VARCHAR,image BLOB)")

                val sqlString= "INSERT INTO recipes (recipename, ingredients, instruction, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,recipeName)
                statement.bindString(2,ingredients)
                statement.bindString(3,instruction)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e:Exception){
                e.printStackTrace()
            }

            val intent= Intent(this@RecipeActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }

    }

    private fun makeSmallerBitmap(image: Bitmap,maximumSize:Int):Bitmap{

        var width =image.width
        var height=image.height

        val bitmapRatio:Double =width.toDouble()/height.toDouble()

        if(bitmapRatio>1){
            width=maximumSize
            val scaledHeight=width/bitmapRatio
            height=scaledHeight.toInt()

        }else{
            height=maximumSize
            val scaledWidth=height*bitmapRatio
            width=scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){

            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()
                }else{
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }

            }else{
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }else{
            if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
                }else{
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            }else{
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }

    }

    private fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode== RESULT_OK){
                val intentFromResult =result.data
                if(intentFromResult!=null){
                    val imageData=intentFromResult.data
                    //binding.imageView.setImageURI(imageData)
                    if(imageData != null){
                        try {
                            if (Build.VERSION.SDK_INT>=28){
                                val source=ImageDecoder.createSource(this@RecipeActivity.contentResolver,imageData)
                                selectedBitmap=ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }

                }
            }

        }
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this@RecipeActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }
        }
    }
}