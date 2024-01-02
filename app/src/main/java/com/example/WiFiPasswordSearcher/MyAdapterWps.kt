package com.example.WiFiPasswordSearcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.*

class MyAdapterWps(context: Context, arr: ArrayList<ItemWps>?) : BaseAdapter() {
    var data = ArrayList<ItemWps>()
    var context: Context
    override fun getCount(): Int { // TODO Auto-generated method stub
        return data.size
    }

    override fun getItem(num: Int): Any { // TODO Auto-generated method stub
        return data[num]
    }

    override fun getItemId(arg0: Int): Long {
        return arg0.toLong()
    }

    override fun getView(i: Int, someView: View?, arg2: ViewGroup): View {
        //Если someView (View из ListView) вдруг оказался равен
        //null тогда мы загружаем его с помошью inflater
        val someView = someView ?: LayoutInflater.from(context).inflate(R.layout.list_wps_item, arg2, false)
        val pin = someView.findViewById(R.id.txtPin) as TextView
        val metod = someView.findViewById(R.id.txtMetod) as TextView
        val score = someView.findViewById(R.id.txtScor) as TextView
        val db = someView.findViewById(R.id.txtDb) as TextView
        //
        pin.text = data[i].header
        metod.text = data[i].subHeader
        score.text = data[i].score
        db.text = data[i].db
        return someView
    }

    init {
        if (arr != null) {
            data = arr
        }
        this.context = context
    }
}