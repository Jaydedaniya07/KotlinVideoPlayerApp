package com.app.videostream.UI.Activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.app.videostream.databinding.ActivityMainBinding
import com.app.videostream.interfaces.OnClickHandler

class MainActivity : BaseActivity<ActivityMainBinding>(), OnClickHandler {
    
    override fun getLayout(inflater: LayoutInflater) = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.setOnClickHandler = this
    }

    override fun initView() {


    }

    override fun onViewClicked(view: View) {
        when(view.id){

        }
    }
}