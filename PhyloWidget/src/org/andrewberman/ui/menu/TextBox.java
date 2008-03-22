package org.andrewberman.ui.menu;

import java.awt.event.KeyEvent;

import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.LayoutUtils;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.TextField;

import processing.core.PApplet;

public class TextBox extends Menu
{
	protected PApplet parentApplet;
	public TextField tf;
	
	public TextBox(PApplet p)
	{
		super(p);
		tf = new TextField(p) {
			@Override
			public void keyEvent(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					performAction();
				}
				super.keyEvent(e);
			}
		};
	}
	
	@Override
	public synchronized void layout()
	{
		float px = menu.style.getF("f.padX");
		float py = menu.style.getF("f.padY");
		
		super.layout();
		tf.setWidth(width-px);
		tf.setHeight(height-py);
		LayoutUtils.centerVertical(tf, y, y+height);
		LayoutUtils.centerHorizontal(tf, x,x+width);
	}
	
	@Override
	public void performAction()
	{
		super.performAction();
		FocusManager.instance.removeFromFocus(tf);
	}
	
	@Override
	public void setWidth(float width)
	{
		super.setWidth(width);
	}
	
	
	@Override
	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
	}
	
	@Override
	protected boolean containsPoint(Point p)
	{
		return false;
	}

	@Override
	public MenuItem create(String label)
	{
		return null;
	}

}