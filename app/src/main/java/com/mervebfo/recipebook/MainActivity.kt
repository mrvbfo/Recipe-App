package com.mervebfo.recipebook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mervebfo.recipebook.databinding.ActivityMainBinding
import com.mervebfo.recipebook.databinding.ActivityRecipeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var recipeList: ArrayList<Recipe>
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        recipeList=ArrayList<Recipe>()

        recipeAdapter=RecipeAdapter(recipeList)
        binding.recyclerView.layoutManager=LinearLayoutManager(this)
        binding.recyclerView.adapter= recipeAdapter
        try{
            val database=this.openOrCreateDatabase("Recipes", MODE_PRIVATE,null)
            val cursor=database.rawQuery("SELECT * FROM recipes",null)
            val recipeNameIx=cursor.getColumnIndex("recipename")
            val idIX=cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val name =cursor.getString(recipeNameIx)
                val id =cursor.getInt(idIX)
                val recipe =Recipe(name, id)
                recipeList.add(recipe)
            }

            recipeAdapter.notifyDataSetChanged()
            cursor.close()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        //Inflater
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.recipemenu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.add_recipe_item) {
            val intent = Intent(this,RecipeActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }




}