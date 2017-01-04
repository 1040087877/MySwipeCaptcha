package looklook.lyb.com.myswipecaptcha.View;

import android.graphics.Path;
import android.graphics.PointF;

/**
 * Created by 10400 on 2017/1/4.
 */

public class DrawHelperUtils {

    /**
     * 传入起点、终点 坐标、凹凸和Path。
     * 会自动绘制凹凸的半圆弧
     *
     * @param start 起点坐标
     * @param end   终点坐标
     * @param path  半圆会绘制在这个path上
     * @param outer 是否凸半圆
     */
    public static void drawPartCircle(PointF start, PointF end, Path path, boolean outer) {
        //三阶贝塞尔曲线，固定写法
        float c = 0.551915024494f;
        //中点 圆心
        PointF middle = new PointF(start.x + (end.x - start.x) / 2, start.y + (end.y - start.y) / 2);
        //半径
        float r1 = (float) Math.sqrt(Math.pow((start.x - middle.x), 2) + Math.pow(start.y - middle.y, 2));
        //gap值
        float gap1 = r1 * c;

        //判断方向
        if (start.x == end.x) {
            //垂直方向
            boolean topToBottom = end.y - start.y > 0 ? true : false;
            int flag;
            if (topToBottom) {
                flag = 1;
            } else {
                flag = -1;
            }
            if (outer) {
                path.cubicTo(start.x+gap1*flag,start.y,
                        middle.x+r1*flag,middle.y-gap1*flag
                ,middle.x+r1*flag,middle.y);
                path.cubicTo(middle.x+r1*flag,middle.y+gap1*flag,end.x+gap1*flag,end.y,end.x,end.y);
            }else {
                path.cubicTo(start.x-gap1*flag,start.y,
                        middle.x-r1*flag,middle.y-gap1*flag
                        ,middle.x-r1*flag,middle.y);
                path.cubicTo(middle.x-r1*flag,middle.y+gap1*flag,end.x-gap1*flag,end.y,end.x,end.y);
            }
        }else {
            //是否是从左到右
            boolean leftToRight = end.x - start.x > 0 ? true : false;
            int flag;
            if(leftToRight){
                flag=1;
            }else {
                flag=-1;
            }
            if(outer){
                //凸 两个半圆
                path.cubicTo(start.x, start.y - gap1 * flag,
                        middle.x - gap1 * flag, middle.y - r1 * flag,
                        middle.x, middle.y - r1 * flag);
                path.cubicTo(middle.x + gap1 * flag, middle.y - r1 * flag,
                        end.x, end.y - gap1 * flag,
                        end.x, end.y);
            }else {
                //凹 两个半圆
                path.cubicTo(start.x, start.y + gap1 * flag,
                        middle.x - gap1 * flag, middle.y + r1 * flag,
                        middle.x, middle.y + r1 * flag);
                path.cubicTo(middle.x + gap1 * flag, middle.y + r1 * flag,
                        end.x, end.y + gap1 * flag,
                        end.x, end.y);
            }
        }


    }
}
