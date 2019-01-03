package com.vodafone.lib.seclibng;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.messaging.RemoteMessage;
import com.vodafone.lib.seclibng.comms.Config;
import com.vodafone.lib.seclibng.comms.Logger;
import com.vodafone.lib.seclibng.encryption.KeytoolHelper;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * An aspect for handling metrics logging for click events,user interactions etc
 */
@Aspect
public class MetricsAspect {
    private static final String TAGMETRICASPECT = "MetricAspect";
    private static final String CLICKEDON = "Clicked on ";
    private static final String SELECTED = "Selected ";

    /**
     * This pointcut and method will find any onClick method
     * and find the {@link View} associated with the click. Using the View's
     * {@link Resources} access the method finds the human-friendly ID of the
     * View and logs it.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onClick(..))")
    public void onClick(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 1) {
            if (args[0] instanceof View) {
                final View view = (View) args[0];
                addToSecLibWithEventType(view, CLICKEDON, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
            }
        }
    }

    /**
     * This pointcut and method will find doClick method supported by ButterKnife
     * and find the {@link View} associated with the click. Using the View's
     * {@link Resources} access the method finds the human-friendly ID of the
     * View and logs it.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* doClick(..))")
    public void onButtonClick(final JoinPoint joinPoint) {

        Object[] args = joinPoint.getArgs();
        if (args.length == 1) {
            if (args[0] instanceof View) {
                final View view = (View) args[0];
                addToSecLibWithEventType(view, CLICKEDON, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
            }
        }
    }

    /**
     * This pointcut and method will find any On long click for any views
     * and find the {@link View} associated with the click. Using the View's
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onLongClick(..))")
    public void onLongClick(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length == 1) {
                if (args[0] instanceof View) {
                    final View view = (View) args[0];
                    addToSecLibWithEventType(view, CLICKEDON, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
                }
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "onLongClick() not a valid listener " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onclick in dialog android.
     * and find the {@link View} associated with the click.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onClick(* ,*))")
    public void onClickDialog(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            if (args[0] instanceof DialogInterface && args[1] instanceof Integer) {
                int buttonId = (int) args[1];
                String buttonName;
                switch (buttonId) {
                    case -1:
                        buttonName = "Positive";
                        break;
                    case -2:
                        buttonName = "Negative";
                        break;
                    default:
                        buttonName = "Neutral";
                        break;
                }
                String eventElement = "AlertDialog";
                String eventDescription = CLICKEDON + buttonName + " button";
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
            }
        }
    }

    /**
     * This pointcut and method will find any onTouch
     * and find the {@link View} associated with the onTouch. Using the View's
     * {@link android.content.res.Resources} access the method finds the human-friendly ID of the
     * View and logs it.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onTouch(..))")
    public void onTouch(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof View) {
            final View view = (View) args[0];
            addToSecLibWithEventType(view, CLICKEDON, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
        }
    }

    /**
     * This pointcut and method will find any onCheckedChange
     * logs it.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @After("execution(* onCheckedChanged(..)) ")
    public void logMetricsOnradioButton(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 2) {
            if (args[0] instanceof View) {
                final View view = (View) args[0];
                addToSecLibWithEventType(view, "Clicked", Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
            }
        }
    }


    /**
     * This pointcut and method will find any Onitemselected
     * and find the {@link View} associated with the  Onitemselected. Using the View's
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onItemSelected(..)) ")

    public void logMetricsOnOnitemselected(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof View) {
            final View view = (View) args[0];
            addToSeclib(view, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
        }
    }

    /**
     * This pointcut and method will find the rating bar change in
     * and find the Rating value
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onRatingChanged(..)) ")
    public void logMetricsOnRatingChanged(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof View) {
            final View view = (View) args[0];
            addToSeclib(view, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
        }
    }

    /**
     * This pointcut and method will find the seek bar change
     * and find the seek value
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onStopTrackingTouch(..)) ")

    public void logMetricsOnStopTrackingTouch(final JoinPoint joinPoint) {

        Object[] args = joinPoint.getArgs();
        if (args.length == 1) {
            if (args[0] instanceof View) {
                final View view = (View) args[0];
                addToSeclib(view, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()));
            }
        }

    }

    /**
     * This pointcut and method will find any option menu item selected
     * and find the {@link View} associated with the click.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */

    @Before("execution(* onOptionsItemSelected(..)) ")

    public void logMetricsOnOptionsItemSelected(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof MenuItemImpl) {
            try {
                MenuItemImpl menuItem = (MenuItemImpl) args[0];
                @SuppressLint("RestrictedApi") String eventDescription = CLICKEDON + "Menu " + menuItem.getTitle().toString();
                @SuppressLint("RestrictedApi") String eventElement = menuItem.getClass().getSimpleName() + ":" + menuItem.getTitleCondensed();
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
            } catch (Exception e) {
                Logger.e("MenuClickException", "Exception in menu click logging " + e.getMessage());
            }
        }
    }

    /**
     * This pointcut and method will find any onNavigationItemSelected menu item selected
     * and find the {@link View} associated with the click.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */

    @Before("execution(* onNavigationItemSelected(..)) ")

    public void logMetricsOnNavigationItemSelected(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof MenuItemImpl) {
            try {
                MenuItemImpl menuItem = (MenuItemImpl) args[0];
                @SuppressLint("RestrictedApi") String eventDescription = CLICKEDON + "Menu " + menuItem.getTitle().toString();
                @SuppressLint("RestrictedApi") String eventElement = "NavigationItem" + ":" + menuItem.getTitleCondensed();
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
            } catch (Exception e) {
                Logger.e("MenuClickException", "Exception in menu click logging " + e.getMessage());
            }
        }
    }

    /**
     * This pointcut and method will find any On item click or onList item click for list view items
     * and find the {@link View} associated with the click.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onItemClick(..)) || execution(* onListItemClick(..)) ")
    public void logMetricsOnItemClick(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof AdapterView && args[0] instanceof View) {
            try {
                String eventDescription = CLICKEDON + "Position " + (Integer.parseInt(args[2].toString()) + 1);
                View view = (View) args[0];
                String idName = view.getResources().getResourceEntryName(view.getId());
                String eventElement = view.getClass().getSimpleName() + ":" + idName;
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
            } catch (Exception e) {
                Logger.e("onItemClickException", "Exception in onItemClick " + e.getMessage());
            }
        }
    }

    /**
     * This pointcut and method will find any On item Long click for List view item
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onItemLongClick(..)) ")
    public void logMetricsOnItemLongClick(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof AdapterView && args[0] instanceof View) {
            String eventDescription = CLICKEDON + "Position " + (Integer.parseInt(args[2].toString()) + 1);
            View view = (View) args[0];
            String idName = view.getResources().getResourceEntryName(view.getId());
            String eventElement = view.getClass().getSimpleName() + ":" + idName;
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
        }
    }

    /**
     * This pointcut and method will find any On tab selected for tab
     * and find the page which is selected
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onTabSelected(..)) ")
    public void logMetricsAddOnTabSelectedListener(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof android.support.design.widget.TabLayout.Tab) {
            TabLayout.Tab tab = (TabLayout.Tab) args[0];
            String eventDescription = "Selected Tab " + (tab.getPosition() + 1);
            String eventElement = tab.getClass().getSimpleName();
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
        }
    }

    /**
     * This pointcut and method will find any Page select for View Pager
     * and find page which is selected.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onPageSelected(..)) ")
    public void logMetricsOnPageSelected(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        try {
            String eventDescription = "Selected Page " + (Integer.parseInt(args[0].toString()) + 1);
            String eventElement = "ViewPager";
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CHANGE);
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "OnPageSelected Error " + e.getMessage(), e);
        }
    }

    /**
     * This pointcut and method will find any On Focus Change
     * and find the {@link View} associated with the view.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */

    @Before("execution(* onFocusChange(..)) ")
    public void logMetricsOnFocusChange(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof View) {
            boolean focus = (boolean) args[1];
            View v = (View) args[0];
            String idName = v.getResources().getResourceEntryName(v.getId());
            String eventDescription = ((focus) ? "Gained" : "Lost") + " Focus";
            String eventElement = v.getClass().getSimpleName() + ":" + idName;
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
        }
    }

    /**
     * This pointcut and method will find any On date Set for Date picker
     * and find the date which is selected.
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */

    @Before("execution(* onDateSet(..)) ")
    public void logMetricsOnDateSet(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof DatePicker) {
            DatePicker datePicker = (DatePicker) args[0];
            String idName = datePicker.getResources().getResourceEntryName(datePicker.getId());
            String eventDescription = "Selected Date(YYYY-MM-DD) : " + args[1] + "-" + ((int) args[2] + 1) + "-" + args[3];
            String eventElement = datePicker.getClass().getSimpleName() + ":" + idName;
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
        }
    }

    /**
     * This pointcut and method will find any On time Set for time picker
     * and find the {@link View} time which is selected
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onTimeSet(..)) ")
    public void logMetricsOnTimeSet(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof TimePicker) {
            TimePicker timePicker = (TimePicker) args[0];
            String idName = timePicker.getResources().getResourceEntryName(timePicker.getId());
            String eventDescription = "Selected Time(HH:MM) : " + args[1] + ":" + args[2];
            String eventElement = timePicker.getClass().getSimpleName() + ":" + idName;
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_CLICKS);
        }
    }

    /**
     * This pointcut and method will find any on Key shortcut for key press
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onKeyShortcut(..)) ")
    public void logOnKeyShortcut(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
        keyEvent(args, classname);
    }

    /**
     * This pointcut and method will find any onKeyLongPress for key press
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onKeyLongPress(..)) ")
    public void logOnKeyLongPress(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
        keyEvent(args, classname);
    }

    /**
     * This pointcut and method will find any onKeyUp for key press
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onKeyUp(..)) ")
    public void logOnKeyUp(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
        keyEvent(args, classname);
    }

    /**
     * This pointcut and method will find any onKey for key press
     *
     * @param joinPoint {@link JoinPoint} representing the method being hijacked
     */
    @Before("execution(* onKey(..)) ")
    public void logOnKeyPress(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
        keyEvent(args, classname);
    }

    /***
     * Key event Element will be identified and added to event.
     *
     * @param args      Joint point args
     * @param className parent class for the key event occurred
     */
    private void keyEvent(Object[] args, String className) {
        try {
            if (args != null && args.length == 2) {
                KeyEvent keyEvent = (KeyEvent) args[1];
                String eventDescription = Config.DEFAULT_NA;
                String eventElement = KeyEvent.keyCodeToString(keyEvent.getKeyCode());
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, className, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "Exception for key event " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onRefresh for swipe refresh widget
     */
    @Before("execution(* onRefresh()) ")
    public void logMetricsOnRefresh(final JoinPoint joinPoint) {

        String eventDescription = "Swiped page for refresh";
        String eventElement = "SwipeRefreshLayout";
        SecLibNG.getInstance().logEventButton(eventElement, eventDescription, Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName()), Event.EventType.UI_SWIPE);
    }

    /**
     * This pointcut and method will find any date change for CalenderView
     */
    @Before("execution(* onSelectedDayChange(..)) ")
    public void onSelectedDayChange(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof CalendarView) {
            CalendarView calendarView = (CalendarView) args[0];
            String idName = calendarView.getResources().getResourceEntryName(calendarView.getId());
            String eventDescription = "Selected Date(YYYY-MM-DD) : " + args[1] + "-" + ((int) args[2] + 1) + "-" + args[3];
            String eventElement = calendarView.getClass().getSimpleName() + ":" + idName;
            String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription, classname, Event.EventType.UI_CLICKS);
        }
    }


    /**
     * This pointcut and method will find any onGroupClick for Expandable List view for Chronometer
     */
    @Before("execution(* onGroupClick(..)) ")
    public void onGroupClick(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof ExpandableListView) {
                ExpandableListView expListVew = (ExpandableListView) args[0];
                String idName = expListVew.getResources().getResourceEntryName(expListVew.getId());
                String eventDescription = "Clicked on Group " + ((Integer.parseInt(args[2].toString()) + 1));
                String eventElement = expListVew.getClass().getSimpleName() + ":" + idName;
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "OnGroupClick Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onChildClick for Expandable List view
     */
    @Before("execution(* onChildClick(..)) ")
    public void onChildClick(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof ExpandableListView) {
                ExpandableListView expListVew = (ExpandableListView) args[0];
                String idName = expListVew.getResources().getResourceEntryName(expListVew.getId());
                String eventDescription = "Clicked on Child " + ((Integer.parseInt(args[3].toString()) + 1) + " of Group " + ((Integer.parseInt(args[2].toString()) + 1)));
                String eventElement = expListVew.getClass().getSimpleName() + ":" + idName;
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "OnChildClick Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onGroupCollapse for Expandable list view
     */
    @Before("execution(* onGroupCollapse(..)) ")
    public void onGroupCollapse(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof Integer) {
                String eventDescription = "Collapsed Group " + (Integer.parseInt(args[0].toString()) + 1);
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton("ExpandableListView", eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "onGroupCollapse Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onGroupCollapse for Expandable list view
     */
    @Before("execution(* onGroupExpand(..)) ")
    public void onGroupExpand(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof Integer) {
                String eventDescription = "Expanded Group " + (Integer.parseInt(args[0].toString()) + 1);
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton("ExpandableListView", eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "onGroupExpanded Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onValueChange for NumberPicker
     */
    @Before("execution(* onValueChange(..)) ")
    public void onValueChange(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof android.widget.NumberPicker) {
                NumberPicker numberPicker = (NumberPicker) args[0];
                String idName = numberPicker.getResources().getResourceEntryName(numberPicker.getId());
                String eventDescription = "Selected Position " + (Integer.parseInt(args[1].toString()) + 1);
                String eventElement = numberPicker.getClass().getSimpleName() + ":" + idName;
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "OnValueChange Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method will find any onMenuItemClick for pop up window
     */
    @Before("execution(* onMenuItemClick(..)) ")
    public void onMenuItemClick(final JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args[0] instanceof MenuItem) {
                MenuItem menuItem = (MenuItem) args[0];
                String idName = menuItem.getTitle().toString();
                String eventDescription = "Clicked on Menu " + idName;
                String eventElement = menuItem.getClass().getSimpleName() + ":" + idName;
                String classname = Config.formattedClassName(joinPoint.getSignature().getDeclaringType().getName());
                SecLibNG.getInstance().logEventButton(eventElement, eventDescription, classname, Event.EventType.UI_CLICKS);
            }
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "onMenuItemClick Exception " + e.getMessage());
        }
    }

    /**
     * This pointcut and method for pushNotification messages
     */
    @Before("execution(* onMessageReceived(*)) ")
    public void onMessageReceived(final JoinPoint joinPoint) {
        if(joinPoint != null) {
            Object arg = joinPoint.getArgs()[0];
            if(arg instanceof RemoteMessage) {
                try {
                    if (KeytoolHelper.getDecKey() != null)
                        SecLibNG.getInstance().logEventButton("NA", "Notification received", "NA", Event.EventType.NOTIFICATION);
                } catch (Exception e) {
                    Logger.e(TAGMETRICASPECT, "OnMessageReceived exception " + e.getMessage());
                }
            }
        }
    }

    /***
     * Add to seclib
     *
     * @param view View object
     */
    private void addToSeclib(View view, String className) {
        String idName = "";
        try {
            idName = view.getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "Resource not available");
            idName = "";
        }
        try {
            String eventElement = (("").equalsIgnoreCase(idName)) ? view.getClass().getSimpleName() : view.getClass().getSimpleName() + ":" + idName;
            String eventDescription_ = "";
            if (view instanceof Spinner) {
                Spinner sp = (Spinner) view;
                eventDescription_ = SELECTED + "item " + (sp.getSelectedItemPosition() + 1);
            } else if (view instanceof SeekBar) {
                SeekBar seekBar = (SeekBar) view;
                eventDescription_ = SELECTED + "value " + (seekBar.getProgress() + "/" + seekBar.getMax());
            } else if (view instanceof RatingBar) {
                RatingBar ratingBar = (RatingBar) view;
                eventDescription_ = SELECTED + "rating " + (ratingBar.getRating() + "/" + ratingBar.getMax());
            } else if (view instanceof MenuItem) {
                eventDescription_ = SELECTED + "Option Menu";
            }
            SecLibNG.getInstance().logEventButton(eventElement, eventDescription_, className, Event.EventType.UI_CLICKS);
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "Exception while adding object: " + e.getMessage(), e);
        }
    }

    /***
     * Add event details to SecLib
     *
     * @param view      View object
     * @param eventName Event name
     * @param className Class name for the event
     */

    private void addToSecLibWithEventType(View view, String eventName, String className) {
        String idName;
        try {
            idName = view.getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            Logger.e(TAGMETRICASPECT, "Resource not available " + e.getMessage());
            idName = "";
        }
        String eventElement = (("").equalsIgnoreCase(idName)) ? view.getClass().getSimpleName() : view.getClass().getSimpleName() + ":" + idName;
        String eventDesc;
        if (view instanceof RadioButton || view instanceof CheckBox) {
            CompoundButton cb = (CompoundButton) view;
            String checked = cb.isChecked() ? "Checked on " : "Unchecked on ";
            eventDesc = checked + cb.getText().toString();
        } else if (view instanceof ToggleButton) {
            CompoundButton cb = (CompoundButton) view;
            String viewState = cb.isChecked() ? Config.COMPOUND_BUTTON_STATE_ON : Config.COMPOUND_BUTTON_STATE_OFF;
            if(cb.getText() != null && !cb.getText().toString().isEmpty()) {
                eventDesc = CLICKEDON + cb.getText().toString() + ": " + viewState;
            } else {
                eventDesc = viewState;
            }
        } else if (view instanceof Switch) {
            CompoundButton cb = (CompoundButton) view;
            String viewState = cb.isChecked() ? Config.COMPOUND_BUTTON_STATE_ON : Config.COMPOUND_BUTTON_STATE_OFF;
            if(cb.getText() != null && !cb.getText().toString().isEmpty()) {
                eventDesc = CLICKEDON + cb.getText().toString() + ": " + viewState;
            } else {
                eventDesc = viewState;
            }
        } else if (view instanceof Button) {
            eventDesc = CLICKEDON + ((Button) view).getText().toString();
        } else if (view instanceof TextView) {
            eventDesc = CLICKEDON + ((TextView) view).getText().toString();
        } else if (view instanceof FloatingActionButton) {
            eventDesc = CLICKEDON + idName;
        } else if (view instanceof RelativeLayout || view instanceof LinearLayout || view instanceof FrameLayout) {
            eventDesc = Config.DEFAULT_NA;

        } else if (view instanceof AppCompatImageButton || view instanceof ImageView) {
            eventDesc = idName.isEmpty() ? Config.DEFAULT_NA : CLICKEDON + idName;
        } else {
            eventDesc = eventName + view.getClass().getSimpleName();
        }
        SecLibNG.getInstance().logEventButton(eventElement, eventDesc, className, Event.EventType.UI_CLICKS);
    }
}