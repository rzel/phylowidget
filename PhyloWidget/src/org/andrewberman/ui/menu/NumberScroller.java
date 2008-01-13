package org.andrewberman.ui.menu;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;

import processing.core.PApplet;

public class NumberScroller extends MenuItem
{
	private float value = 0, defaultValue = 0, increment = 1, scrollSpeed = 1;
	private float min = -Float.MAX_VALUE, max = Float.MAX_VALUE;
	// private int minDigits, maxDigits;
	private String stringValue;
	private DecimalFormat df;

	private Field field;
	private Object fieldObj;
	private boolean useReflection;

	private float tWidth, nWidth, nOffset;
	boolean scrolling;
	float startY, startVal;

	public NumberScroller()
	{
		super();

		df = new DecimalFormat("#######0.0#");
		df.setDecimalSeparatorAlwaysShown(false);

		setIncrement(increment);
		setDefault(defaultValue);
		setValue(value);
		setScrollSpeed(scrollSpeed);
		
		stringValue = new String();
	}

	public float getValue()
	{
		float oldValue = value;
		try
		{
			if (useReflection)
				value = field.getFloat(fieldObj);
		} catch (Exception e)
		{
			useReflection = false;
			e.printStackTrace();
		}
		if (value != oldValue)
			updateString();
		return value;
	}

	void updateString()
	{
		stringValue = df.format(value);
	}

	public void setDefault(float def)
	{
		defaultValue = def;
		setValue(defaultValue);
	}

	public void setMin(float min)
	{
		this.min = min;
	}

	public void setMax(float max)
	{
		this.max = max;
	}

	public void setProperty(Object obj, String prop)
	{
		try
		{
			field = obj.getClass().getField(prop);
			fieldObj = obj;
			useReflection = true;
		} catch (Exception e)
		{
			e.printStackTrace();
			field = null;
			return;
		}
		setValue(defaultValue);
	}

	public void setIncrement(float inc)
	{
		increment = inc;
		/*
		 * Try and auto-detect number of decimal places from the increment.
		 */
		int numDecimals = (int) Math.ceil((float) -Math.log10(increment));
		// System.out.println(Math.log10(increment)+ " "+getName() + " " +
		// increment + " " + numDecimals);
		df.setMinimumFractionDigits(numDecimals);
		df.setMaximumFractionDigits(numDecimals);

		setValue(value);
	}

	public void setScrollSpeed(float changePerPixel)
	{
		scrollSpeed = changePerPixel;
	}

	protected void drawMyself()
	{
		super.drawMyself();
		getValue();
		if (scrolling)
		{
			/*
			 * Cause the menu to re-layout in case we've changed preferred size.
			 */
			menu.layout();
		}

		float curX = x + menu.style.padX;
		MenuUtils.drawLeftText(this, getName() + ":", curX);
		curX += tWidth;

		curX = getX() + getWidth() - menu.style.padX - nWidth;
		MenuUtils.drawSingleGradientRect(this, curX, y, nWidth, height);
		/*
		 * update the "value" object using Reflection.
		 */
		MenuUtils.drawText(this, stringValue, true, true, curX, y, nWidth,
				height);
	}

	public void setValue(float val)
	{
		float oldValue = value;
		value = PApplet.constrain(val, min, max);
		if (useReflection)
		{
			try
			{
				field.setFloat(fieldObj, value);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		updateString();
	}

	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
		/*
		 * For the height, let's use the height of some capital letters.
		 */
		float tHeight = UIUtils.getTextHeight(menu.buff, menu.style.font,
				menu.style.fontSize, "XYZ", true);
		/*
		 * Calculate the text rectangle size.
		 */
		if (getName().length() > 0)
		{
			tWidth = UIUtils.getTextWidth(menu.buff, menu.style.font,
					menu.style.fontSize, getName() + ":", true);
			tWidth += menu.style.padX;
		}

		String s = stringValue;

		/*
		 * Store the beginning point for the number area.
		 */
		nWidth = 0;
		nWidth += UIUtils.getTextWidth(menu.buff, menu.style.font,
				menu.style.fontSize * .75f, s, true);
		nWidth += 2 * menu.style.padX;

		nOffset = getWidth() - menu.style.padX - nWidth;

		setWidth(menu.style.padX + tWidth + nWidth + menu.style.padX);
		setHeight(tHeight + 2 * menu.style.padY);
	}

	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		buff.setFrame(x, y, width, height);
		Rectangle2D.union(rect, buff, rect);
		super.getRect(rect, buff);
	}

	protected void performAction()
	{
		// super.performAction();
	}

	protected void visibleMouseEvent(MouseEvent e, Point tempPt)
	{
		super.visibleMouseEvent(e, tempPt);

		if (mouseInside)
		{
			menu.setCursor(Cursor.N_RESIZE_CURSOR);
		}
		switch (e.getID())
		{
			case (MouseEvent.MOUSE_PRESSED):
				if (mouseInside)
				{
					if (e.getClickCount() > 1)
					{
						setValue(defaultValue);
					}
					startY = tempPt.y;
					startVal = getValue();
					scrolling = true;
					FocusManager.instance.setModalFocus(this.menu);
				}
				break;
			case (MouseEvent.MOUSE_DRAGGED):
				if (scrolling)
				{
					float dy = startY - tempPt.y;
					float dVal = dy * increment * scrollSpeed;
					value = startVal + dVal;
					setValue(value);
					e.consume();
				}
				break;
			case (MouseEvent.MOUSE_RELEASED):
				if (scrolling)
				{
					e.consume();
					scrolling = false;
					FocusManager.instance.removeFromFocus(this.menu);
				}
				break;
		}
	}

	static RoundRectangle2D.Float buffRoundRect = new RoundRectangle2D.Float(0,
			0, 0, 0, 0, 0);

	protected boolean containsPoint(Point p)
	{
		if (scrolling)
			return true;
		buffRoundRect.setRoundRect(x, y, width, height, menu.style.roundOff,
				menu.style.roundOff);
		// buffRoundRect.setRoundRect(x + nOffset, y, nWidth, height,
		// menu.style.roundOff, menu.style.roundOff);
		return buffRoundRect.contains(p);
	}

}
