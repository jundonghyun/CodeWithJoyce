package com.example.qrcodereader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.qrcodereader.databinding.ActivityResultAcitivtyBinding

class ResultAcitivty : AppCompatActivity() {

    lateinit var binding: ActivityResultAcitivtyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultAcitivtyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val result = intent.getStringExtra("msg")

        setUI(result)
    }

    private fun setUI(result: String?) {
        binding.tvContent.text = result
        binding.btnGoBack.setOnClickListener {
            finish()
        }
    }


}