package com.jitu.fluidspeech;
import android.os.*;import android.view.*;
/** A drag never becomes a tap. Hold-to-record waits briefly so moving does not open the mic. */
final class BubbleGestures implements View.OnTouchListener {
 interface Actions {boolean tapMode();boolean startHold();void stopHold();void cancelHold();void tap();void drag(float dx,float dy);void dropped();void aborted();}
 private final Actions actions;private final Handler handler=new Handler(Looper.getMainLooper());private final float slop;
 private float x,y;private boolean dragging,holding,down;private final Runnable hold;
 BubbleGestures(Actions a,float threshold){actions=a;slop=threshold;hold=()->{if(down&&!dragging)holding=actions.startHold();};}
 public boolean onTouch(View view,MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:down=true;dragging=false;holding=false;x=e.getRawX();y=e.getRawY();if(!actions.tapMode())handler.postDelayed(hold,220);return true;
   case MotionEvent.ACTION_MOVE:
    float dx=e.getRawX()-x,dy=e.getRawY()-y;
    if(!dragging&&Math.hypot(dx,dy)>slop){dragging=true;handler.removeCallbacks(hold);if(holding){actions.cancelHold();holding=false;}}
    if(dragging){actions.drag(dx,dy);x=e.getRawX();y=e.getRawY();}return true;
   case MotionEvent.ACTION_UP:
    down=false;handler.removeCallbacks(hold);if(dragging)actions.dropped();else if(holding)actions.stopHold();else if(actions.tapMode())actions.tap();holding=false;return true;
   case MotionEvent.ACTION_CANCEL:
    down=false;handler.removeCallbacks(hold);if(holding)actions.cancelHold();if(dragging)actions.aborted();dragging=false;holding=false;return true;
  }return true;
 }
 void clear(){handler.removeCallbacks(hold);down=false;holding=false;}
}
