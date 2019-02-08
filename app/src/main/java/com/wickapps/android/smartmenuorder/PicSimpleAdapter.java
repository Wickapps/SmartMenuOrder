/*
 * Copyright (C) 2019 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wickapps.android.smartmenuorder;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

public class PicSimpleAdapter extends BaseAdapter {

    Locale lc;
    ViewHolder holder = null;

    private Context context;

    private Activity activity;
    String[] menuItem;
    String[] categoryAll;
    String descLang, item, itemEng, pricenumber;
    ListView listRating;
    int fsize, psizeH, psizeW;
    LinearLayout.LayoutParams lpimg;

    private ArrayList<String> data;

    public PicSimpleAdapter(Context context, int layoutListID, ArrayList<String> d, Activity a) {
        super();
        activity = a;
        this.context = context;
        this.data = d;
        data = d;

        menuItem = Global.MENUTXT.split("\\n");
        categoryAll = Global.CATEGORYTXT.split("\\n");
        fsize = Utils.getFontSize(activity);
        psizeW = (int) (Utils.getWidth(activity) / 9.0);
        psizeH = (int) (psizeW * 1.5);
        lpimg = new LinearLayout.LayoutParams(psizeW, psizeH);
        lpimg.gravity = Gravity.CENTER;
    }

    static class ViewHolder {
        TextView title;
        TextView caption;
        TextView pricenumber;
        TextView newdish;
        TextView popular;
        TextView healthy;
        RatingBar rating;
        ImageView img;
        TextView line;
        LinearLayout line2;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflator.inflate(R.layout.list_complex, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.list_complex_title);
            holder.caption = (TextView) convertView.findViewById(R.id.list_complex_caption);
            holder.pricenumber = (TextView) convertView.findViewById(R.id.list_complex_pricenumber);
            holder.newdish = (TextView) convertView.findViewById(R.id.list_complex_new);
            holder.popular = (TextView) convertView.findViewById(R.id.list_complex_popular);
            holder.healthy = (TextView) convertView.findViewById(R.id.list_complex_healthy);
            holder.img = (ImageView) convertView.findViewById(R.id.foodPic);
            holder.line = (TextView) convertView.findViewById(R.id.lineTop);
            holder.line2 = (LinearLayout) convertView.findViewById(R.id.LLLineTop);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        String line = menuItem[position];
        String[] menuColumns = line.split("\\|");

        String type = menuColumns[0];

        String[] itemColumns = menuColumns[2].split("\\\\");
        String[] descColumns = menuColumns[4].split("\\\\");

        if (!Global.EnglishLang) {
            item = itemColumns[1];
            descLang = descColumns[1];
        } else {
            item = itemColumns[0];
            descLang = descColumns[0];
        }
        itemEng = itemColumns[0];
        pricenumber = menuColumns[6];

        holder.title.setText(item);
        holder.title.setTextSize(fsize);
        holder.title.setTextColor(Global.FontColor);
        holder.caption.setText(descLang);
        holder.caption.setTextSize(fsize);
        holder.caption.setTextColor(Global.FontColor);
        holder.pricenumber.setText(pricenumber);
        holder.pricenumber.setTextSize(fsize);
        holder.pricenumber.setMinimumWidth(psizeW);
        holder.pricenumber.setMaxWidth(psizeW);
        holder.popular.setTextSize(fsize);
        holder.newdish.setTextSize(fsize);

        // set Top divider if start of Category
        if (MenuActivity.MenuPosition.contains(position)) {
            int catTitleIndex = MenuActivity.MenuPosition.indexOf(position);
            if (Global.EnglishLang) {
                holder.line.setText(MenuActivity.CategoryEng.get(catTitleIndex));
            } else {
                holder.line.setText(MenuActivity.CategoryAlt.get(catTitleIndex));
            }
            holder.line.setVisibility(View.VISIBLE);
            holder.line2.setVisibility(View.VISIBLE);
            holder.line.setTextSize(fsize);
            holder.line.setTextColor(Global.LabelFontColor);
            holder.line.getBackground().setColorFilter(Global.LabelColor, PorterDuff.Mode.SRC_OVER);
        } else {
            holder.line.setVisibility(View.GONE);
            holder.line2.setVisibility(View.GONE);
        }

        // turn on POPULAR and NEW and HEALTHY badges if the flags are set
        holder.popular.setVisibility(View.GONE);
        holder.newdish.setVisibility(View.GONE);
        holder.healthy.setVisibility(View.GONE);
        if (Global.ShowBadges) {
            if (type.substring(1, 2).equals("1")) {
                holder.popular.setVisibility(View.VISIBLE);
            }
            if (type.substring(2, 3).equals("1")) {
                holder.newdish.setVisibility(View.VISIBLE);
            }
            if (type.substring(3, 4).equals("1")) {
                holder.healthy.setVisibility(View.VISIBLE);
            }
        }

        // set image
        String imageurl = Global.fetchURL200.get(position);
        //String imageurl = Global.fetchURL800.get(position);
        holder.img.setTag(imageurl);
        holder.img.setScaleType(ScaleType.FIT_CENTER);
        holder.img.setLayoutParams(lpimg);
        // Lazy load the image with Picasso
        Picasso.get()
                .load(imageurl)
                //.resize(50, 50)
                //.centerCrop()
                .placeholder(R.drawable.nopic)
                .error(R.drawable.nopic)
                .into(holder.img);

        if (!Global.ShowPics) {
            holder.img.setVisibility(View.GONE);
        }
        if (!Global.ShowDishDescriptions) {
            holder.caption.setVisibility(View.GONE);
        }
        return convertView;
    }
}
