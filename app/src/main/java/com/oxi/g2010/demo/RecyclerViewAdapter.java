package com.oxi.g2010.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by lei.feng.chn@gmail.com on 2017/9/19.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private ArrayList mList;
    private Context mContext;

    public RecyclerViewAdapter(Context context,ArrayList arrayList) {
        mContext = context;
        mList = arrayList;

    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.finger_id_item,
                parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.mTv.setText(mList.get(position).toString());
    }

    @Override
    public int getItemCount() {
        Log.e("fenglei","size:"+ mList.size());
        return  mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mTv;

        public ViewHolder(View itemView) {
            super(itemView);
            mTv = (TextView) itemView.findViewById(R.id.tv_finger);
        }
    }


}
