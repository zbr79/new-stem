package com.example.final_project

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.final_project.models.BoardSize
import com.example.final_project.models.MemoryCard
import com.example.final_project.models.MemoryGame
import com.example.final_project.utils.EXTRA_BOARD_SIZE
import com.example.final_project.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    private lateinit var rvBoard: RecyclerView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    //Save instance state recycler view NOT WORKING

    private var savedRecyclerLayoutState: Parcelable? = null
    private val BUNDLE_RECYCLER_LAYOUT = "recycler_layout"

    //


    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter : MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        /*
        val intent = Intent(this, CreateActivity::class.java)
        intent.putExtra(EXTRA_BOARD_SIZE,BoardSize.MEDIUM)
        StartActivity(intent)

         */
        setupBoard()

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh ->{
                //Creating warning to player they will lose progress
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog(getString(R.string.quitCurrentGame), null, View.OnClickListener {
                        setupBoard()
                    })
                }else {
                    setupBoard()
                }
            }
            R.id.mi_new_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    private fun showCreationDialog(){
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog(getString(R.string.create_your_own_memory_board),boardSizeView, View.OnClickListener{
            //set a new value for the board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to a new activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })

    }

    private fun showNewSizeDialog(){
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }


        showAlertDialog(getString(R.string.choose_new_size),boardSizeView, View.OnClickListener{
            //set a new value for the board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            setupBoard()
        })
    }
    private fun showAlertDialog(title:String, view: View?,positiveButtonClickListener: View.OnClickListener){
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok)){_,_->
                    positiveButtonClickListener.onClick(null)
            }.show()

    }
    private fun setupBoard()
    {
        when(boardSize){
            BoardSize.EASY ->{
                tvNumMoves.text = getString(R.string.easy)
                tvNumPairs.text = getString(R.string.easyPairsFound)
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = getString(R.string.medium)
                tvNumPairs.text = getString(R.string.mediumPairsFound)
            }
            BoardSize.HARD -> {
                tvNumMoves.text = getString(R.string.hard)
                tvNumPairs.text = getString(R.string.hardPairsFound)
            }
        }
        // Set initial color of num pairs text view to red
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))

        // Construct memory game
        memoryGame = MemoryGame(boardSize)
        /*
        Manager: Measure and position item views
        Adapter: provide a binding for the data set to view of the Recycler View
         */
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            //Create a class of type CardClickListener
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }


    //Update the game after each flip
    private fun updateGameWithFlip(position: Int) {
        //error handling
        if (memoryGame.haveWonGame()){
            //alert user of invalid move
            Snackbar.make(clRoot, getString(R.string.alreadyWon), Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)){
            //alert user of invalid move
            Snackbar.make(clRoot, getString(R.string.invalidMove), Snackbar.LENGTH_SHORT).show()
            return
        }

        //Actually flip over card and display info to user if they found a match or won
        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a match! Number of pairs found: ${memoryGame.numPairsFound}")

            // The more pair user found, the more color change on text View num Pairs
            // Red if there are no pair match, green if all paris is found
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)

            tvNumPairs.text = getString(R.string.numPairs,memoryGame.numPairsFound, boardSize.getNumPairs())
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, getString(R.string.winGame), Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text = getString(R.string.numMoves, memoryGame.getNumMoves())
        adapter.notifyDataSetChanged()
    }

    //App persistence after rotation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        // save recycler view not working
//        val listState: Parcelable? = rvBoard.layoutManager?.onSaveInstanceState()
//        outState.putParcelable("recyclerState", listState)
//        savedRecyclerLayoutState = rvBoard.getLayoutManager()?.onSaveInstanceState()
1

        val numMoves: CharSequence? = tvNumMoves.text
        val numPairs: CharSequence? = tvNumPairs.text
        val savedMemoryGame : MemoryGame = memoryGame
        val savedCards = savedMemoryGame.cards

        val sharedPreferences: SharedPreferences = getSharedPreferences("shared preference", MODE_PRIVATE)
        val editor : SharedPreferences.Editor = sharedPreferences.edit()
        val gson : Gson = Gson()
        val json : String = gson.toJson(savedCards)
        editor.putString("SavedCards",json)
        editor.apply()

        outState.putCharSequence("SaveNumMoves", numMoves)
        outState.putCharSequence("SaveNumPairs", numPairs)

//        Log.d("LOG_TAG_ACTIVITY", "cards in save${savedCards}")
        Log.d("LOG_TAG_ACTIVITY", "Activity has been saved")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)


        val savedMemoryGame : MemoryGame = memoryGame
        val savedRvBoard : RecyclerView
        var savedCards = savedMemoryGame.cards

        val sharedPreferences: SharedPreferences = getSharedPreferences("shared preference", MODE_PRIVATE)
        val gson : Gson = Gson()
        val json : String? = sharedPreferences.getString("SavedCards", null)
        val type : Type = object : TypeToken<List<MemoryCard>>() {}.type
        savedCards = gson.fromJson(json, type)

        if (savedCards == null){
            savedCards = savedMemoryGame.cards
        }
        //recycler view not working
//        val listState = savedInstanceState.getParcelable("recyclerState", null)
//        rvBoard.getLayoutManager()?.onRestoreInstanceState(savedRecyclerLayoutState)

        val numMoves: CharSequence? = savedInstanceState.getCharSequence("SaveNumMoves","0")
        val numPairs: CharSequence? = savedInstanceState.getCharSequence("SaveNumPairs","0")
        tvNumMoves.text = numMoves
        tvNumPairs.text = numPairs

        Log.d("LOG_TAG_ACTIVITY", "Cards in retrieve: $savedCards")
        Log.d("LOG_TAG_ACTIVITY", "Activity has been restored")
    }
//    override fun onPause() {
//        super.onPause()
//        savedRecyclerLayoutState = rvBoard.getLayoutManager()?.onSaveInstanceState()
//    }

//    override fun onResume() {
//        super.onResume()
//        rvBoard.getLayoutManager()?.onRestoreInstanceState(savedRecyclerLayoutState)
//    }
}