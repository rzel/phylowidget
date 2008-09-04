/*******************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * PhyloWidget. If not, see <http://www.gnu.org/licenses/>.
 */
package org.phylowidget.render;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.andrewberman.sortedlist.SortedXYRangeList;
import org.andrewberman.ui.Color;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.TextField;
import org.andrewberman.ui.UIGlobals;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.unsorted.BulgeUtil;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.phylowidget.PhyloTree;
import org.phylowidget.PhyloWidget;
import org.phylowidget.UsefulConstants;
import org.phylowidget.tree.PhyloNode;
import org.phylowidget.tree.RootedTree;

import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PGraphicsJava2D;

/**
 * The abstract tree renderer class.
 * 
 * @author Greg Jordan
 */
@SuppressWarnings("unchecked")
public class BasicTreeRenderer extends DoubleBuffer implements TreeRenderer, GraphListener, UsefulConstants
{
	float baseStroke;

	protected LayoutBase treeLayout = new LayoutUnrooted();

	protected OverlapDetector overlap = new OverlapDetector();

	protected PhyloNode widestNode;

	protected PGraphicsJava2D canvas;

	/**
	 * These variables are set in the calculateSizes() method during every round
	 * of rendering. Very important!
	 */
	protected float colSize;

	/**
	 * Size of the text, as a multiplier relative to normal size.
	 */
	protected float dFont;
	/**
	 * Radius of the node ellipses.
	 */
	protected float dotWidth;

	protected double dx;

	protected double dy;

	/**
	 * Font to be used to draw the nodes.
	 */
	protected PFont font;

	// protected ArrayList<NodeRange> ranges = new ArrayList<NodeRange>();

	/**
	 * Width of the node label gutter.
	 */
	protected float biggestAspectRatio = 0;

	public static NodeRenderer decorator;

	/**
	 * Leaf nodes in the associated tree.
	 */
	//	protected ArrayList<PhyloNode> leaves = new ArrayList<PhyloNode>();
	//	protected ArrayList<PhyloNode> sigLeaves = new ArrayList<PhyloNode>();
	protected PhyloNode[] leaves = new PhyloNode[1];
	protected PhyloNode[] sigLeaves = new PhyloNode[1];

	/**
	 * A data structure to store the rectangular regions of all nodes. Instead
	 * of drawing all nodes, we retrieve the nodes whose regions intersect with
	 * the visible rectangle, and then draw. This can significantly improve
	 * performance when viewing only a portion of a large tree.
	 */
	protected SortedXYRangeList list = new SortedXYRangeList();

	boolean mainRender;

	protected boolean needsLayout;

	/**
	 * All nodes in the associated tree.
	 */
	protected PhyloNode[] nodes = new PhyloNode[1];

	//	protected HashMap<PhyloNode, NodeRange> nodesToRanges = new HashMap<PhyloNode, NodeRange>();

	/**
	 * These variables are set in the calculateSizes() method during every round
	 * of rendering. Very important!
	 */
	//	protected float numCols;
	/**
	 * These variables are set in the calculateSizes() method during every round
	 * of rendering. Very important!
	 */
	//	protected float numRows;
	RenderingHints oldRH;

	protected Point ptemp = new Point(0, 0);

	protected Point ptemp2 = new Point(0, 0);

	/**
	 * Radius of the node ellipses.
	 */
	//	protected float rad;
	/**
	 * The rectangle that defines the area in which this renderer will draw
	 * itself.
	 */
	public Rectangle2D.Float rect, screenRect;

	/**
	 * These variables are set in the calculateSizes() method during every round
	 * of rendering. Very important!
	 */
	protected float rowSize;

	protected double scaleX;

	protected double scaleY;

	/**
	 * Styles for rendering the tree.
	 */

	protected float textSize;

	protected int threshold;

	Rectangle2D.Float tRect = new Rectangle2D.Float();

	/**
	 * The tree that will be rendered.
	 */
	protected RootedTree tree;

	private boolean fforwardMe;

	private float tsf;

	public BasicTreeRenderer()
	{
		rect = new Rectangle2D.Float(0, 0, 0, 0);
		font = UIGlobals.g.getPFont();
		if (decorator == null)
			decorator = new NodeRenderer();

		setOptions();
	}

	float calcRealX(PhyloNode n)
	{
		return (float) (n.getX() * scaleX + dx);
	}

	float calcRealY(PhyloNode n)
	{
		return (float) (n.getY() * scaleY + dy);
	}

	protected void constrainAspectRatio()
	{

	}

	ArrayList<PhyloNode> foundItems = new ArrayList<PhyloNode>();

	private Area a;

	protected void draw()
	{
		float minSize = Math.min(rowSize, colSize);
		baseStroke = getNormalLineWidth() * PhyloWidget.cfg.lineWidth;
		canvas.noStroke();
		canvas.fill(0);

		canvas.textFont(UIGlobals.g.getPFont());
		canvas.textAlign(PConstants.LEFT, PConstants.CENTER);

		hint();
		screenRect = new Rectangle2D.Float(0, 0, canvas.width, canvas.height);
		UIUtils.screenToModel(screenRect);

		treeLayout.drawScaleX = (float) scaleX;
		treeLayout.drawScaleY = (float) scaleY;

		/*
		 * FIRST LOOP: Updating nodes Update all nodes, regardless of
		 * "threshold" status.
		 * Also set each node's drawMe flag to FALSE.
		 */
		a = new Area();
		foundItems.clear();
		int nodesDrawn = 0;
		PhyloNode[] nodesToDraw = new PhyloNode[nodes.length];
		Thread.yield();
		for (int i = 0; i < nodes.length; i++)
		{
			Thread.yield();
			PhyloNode n = nodes[i];
			if (fforwardMe)
				n.fforward();
			updateNode(n);
			n.drawMe = false;
			n.labelWasDrawn = false;
			n.nodeWasDrawn = false;
			n.isWithinScreen = isNodeWithinScreen(n);
			//			System.out.println(n+"   "+n.isWithinScreen);
			n.bulgeFactor = 1;
			if (n.found && n.isWithinScreen)
				foundItems.add(n);
			// GJ 2008-09-03: Add ALWAYS_SHOW nodes to the foundItems list.
			if (n.getAnnotation(UsefulConstants.LABEL_ALWAYSSHOW) != null && n.getAnnotation(UsefulConstants.LABEL_ALWAYSSHOW).equals("1"))
				foundItems.add(0, n);
			if (nodesDrawn >= PhyloWidget.cfg.renderThreshold)
				continue;
			if (!n.isWithinScreen)
				continue;
			n.drawMe = true;
			nodesToDraw[nodesDrawn] = n;
			nodesDrawn++;
		}
		fforwardMe = false;
		//		list.sort();
		/*
		 * SECOND LOOP: Flagging drawn nodes
		 *  -- Now, we go through all nodes
		 * (remember this is a list sorted by significance, i.e. enclosed number
		 * of leaves). At each node, we decide whether or not to draw it based
		 * on whether it is within the screen area. We exit the loop once the
		 * threshold number of nodes has been drawn.
		 */
		//		int nodesDrawn = 0;
		//		ArrayList<PhyloNode> nodesToDraw = new ArrayList<PhyloNode>(nodes
		//				.size());
		//		for (int i = 0; i < nodes.size(); i++)
		//		{
		//			PhyloNode n = nodes.get(i);
		//			// n.isWithinScreen = isNodeWithinScreen(n);
		//			if (nodesDrawn >= PhyloWidget.cfg.renderThreshold)
		//				break;
		//			if (!n.isWithinScreen)
		//				continue;
		//			/*
		//			 * Ok, let's flag this thing to be drawn.
		//			 */
		//			n.drawMe = true;
		//			nodesDrawn++;
		//			nodesToDraw.add(n);
		//		}
		/*
		 * THIRD LOOP: Drawing nodes
		 *   - This loop actually does the drawing.
		 */
		Thread.yield();
		for (int i = nodesDrawn - 1; i >= 0; i--)
		{
			Thread.yield();
			PhyloNode n = nodesToDraw[i];
			if (!n.drawMe)
			{
				if (n.isWithinScreen)
				{
					if (isAnyParentDrawn(n))
						continue;
				} else
					continue;
			}
			/*
			 * Ok, we've skipped all the non-drawn nodes,
			 * let's do the actual (non-label) drawing.
			 */
			NodeRenderer.r = this;
			handleNode(n);
			n.nodeWasDrawn = true;
			//			decorator.lr.render(canvas, n, true);
			//			decorator.nr.renderUntransformed(canvas, n);
		}

		/*
		 * FOURTH LOOP: Drawing labels
		 *   - This loop uses the crazy overlap logic.
		 * 
		 * 
		 */

		/*
		 * If we have a hovered node, always draw it.
		 */
		if (tree instanceof PhyloTree)
		{
			PhyloTree pt = (PhyloTree) tree;
			PhyloNode h = pt.hoveredNode;
			if (h != null)
			{
				Point point = new Point(getX(h), getY(h));
				float dist = (float) point.distance(mousePt);
				float bulgedSize = BulgeUtil.bulge(dist, .7f, 30);
				if (tree.isLeaf(h))
				{
					if (textSize <= 14)
						h.bulgeFactor = bulgedSize;
					else
						h.bulgeFactor = 1f;
				}
				insertAndReturnOverlap(h);
				decorator.renderNode(this, h);
				//				handleNode(h);
				h.labelWasDrawn = true;
			}
		}

		/*
		 * Also always try to draw nodes that are "found".
		 */
		Thread.yield();
		for (PhyloNode n : foundItems)
		{
			Thread.yield();
			NodeRange r = n.range;
			if (!tree.isLeaf(n))
			{
				decorator.lineRender.render(canvas, n, true);
			} else if (tree.isLabelSignificant(n.getLabel()))
			{
				if (insertAndReturnOverlap(n))
					continue;
				decorator.renderNode(this, n);
				n.labelWasDrawn = true;
			}
		}

		/*
		 * Now, go through the significance-sorted list of leaves, drawing and occluding as we go.
		 */
		Thread.yield();
		for (int i = 0; i < sigLeaves.length; i++)
		{
			Thread.yield();
			PhyloNode n = sigLeaves[i];
			if (!n.isWithinScreen || n.labelWasDrawn)
				continue;
			NodeRange r = n.range;
			if (insertAndReturnOverlap(n))
				continue;

			n.labelWasDrawn = true;
			if (!n.nodeWasDrawn)
				decorator.nr.renderUntransformed(canvas, n);
			decorator.renderNode(this, n);
			//			handleNode(n);
		}

		/*
		 * Finally, unhint the canvas.
		 */
		unhint();
	}

	private Polygon tempP = new Polygon();

	private final boolean insertAndReturnOverlap(PhyloNode n)
	{
		//		if (!tree.isLeaf(n)) // Do nothing and pretend no overlap for branch nodes.
		//			return false;
		if (PhyloWidget.cfg.showAllLabels)
			return false;
		float angle = n.getAngle();
		if (angle % Math.PI / 2 == 0 || PhyloWidget.cfg.textRotation == 0)
		{
			if (intersectsRect(n, a))
				return true;
			a.add(new Area(r2d));
		} else
		{
			fillPolygon(n, tempP);
			if (intersectsPoly(n, a, tempP))
				return true;
			a.add(new Area(tempP));
		}
		return false;
	}

	Rectangle2D.Float r2d = new Rectangle2D.Float();
	static final float POLYMULT = 1000;

	private final boolean intersectsPoly(PhyloNode n, Area a, Polygon scratch)
	{
		//		fillPolygon(n, scratch);
		int[] xpoints = scratch.xpoints;
		int[] ypoints = scratch.ypoints;
		for (int i = 0; i < xpoints.length; i++)
		{
			float x = (float) xpoints[i] / POLYMULT;
			float y = (float) ypoints[i] / POLYMULT;
			if (a.contains(x, y))
				return true;
		}
		return false;
	}

	private final boolean intersectsRect(PhyloNode n, Area a)
	{
		//		r2d.setFrame((float)n.getRealX(),(float)n.getRealY(),0,0);
		//		for (Point2D pt : n.corners)
		//		{
		//			r2d.add(pt);
		//		}
		r2d.setFrame(n.range.loX, n.range.loY, n.range.hiX - n.range.loX, n.range.hiY - n.range.loY);
		return a.intersects(r2d);
	}

	private void fillPolygon(PhyloNode n, Polygon p)
	{
		p.reset();
		Point2D[] points = n.corners;
		for (Point2D pt : points)
		{
			p.addPoint((int) (pt.getX() * POLYMULT), (int) (pt.getY() * POLYMULT));
		}
	}

	protected void drawBootstrap(PhyloNode n)
	{
		if (n.isNHX() && PhyloWidget.cfg.showBootstrapValues)
		{
			String boot = n.getAnnotation(BOOTSTRAP);
			if (boot != null)
			{
				canvas.pushMatrix();
				canvas.translate(getX(n), getY(n));
				Double value = Double.parseDouble(boot);
				float curTextSize = textSize * 0.5f;
				canvas.textFont(font);
				canvas.textSize(curTextSize);
				canvas.fill(PhyloWidget.cfg.getTextColor().brighter(100).getRGB());
				canvas.textAlign(canvas.RIGHT, canvas.BOTTOM);
				//				float s = strokeForNode(n) / 2 + rowSize * RenderConstants.labelSpacing;
				float s = 0;
				canvas.text(boot, -getNodeRadius(), -s);
				canvas.popMatrix();
			}
		} else
		{
			return;
		}

	}

	protected boolean useOverlapDetector()
	{
		return true;
	}

	double clamp(double a, double lo, double hi)
	{
		if (a <= lo)
			return lo;
		else if (a >= hi)
			return hi;
		else
			return a;
	}

	protected float getNormalLineWidth()
	{
		float min = rowSize * 0.1f;
		return min;
		//		return min * RenderConstants.labelSpacing;
	}

	public void edgeAdded(GraphEdgeChangeEvent e)
	{
		needsLayout = true;
	}

	public void edgeRemoved(GraphEdgeChangeEvent e)
	{
		needsLayout = true;
	}

	public float getNodeRadius()
	{
		return dotWidth / 2f;
	}

	public float getTextSize()
	{
		return textSize;
	}

	public RootedTree getTree()
	{
		return tree;
	}

	public void fforward()
	{
		ArrayList ffMe = new ArrayList();
		tree.getAll(tree.getRoot(), null, ffMe);
		for (int i = 0; i < ffMe.size(); i++)
		{
			PhyloNode n = (PhyloNode) ffMe.get(i);
			n.fforward();
		}
	}

	float getX(PhyloNode n)
	{
		return n.getRealX();
		// return (float) (n.getX() * scaleX + dx);
	}

	float getY(PhyloNode n)
	{
		return n.getRealY();
		// return (float) (n.getY() * scaleY + dy);
	}

	protected void handleNode(PhyloNode n)
	{
		if (tree.isLeaf(n))
		{
			decorator.lineRender.render(canvas, n, true);
			decorator.nr.renderUntransformed(canvas, n);
		} else
		{
			decorator.lineRender.render(canvas, n, true);
			decorator.nr.renderUntransformed(canvas, n);
			/*
			 * If we're a NHX node, then draw the bootstrap (if the config says so).
			 */
			drawBootstrap(n);

			if (PhyloWidget.cfg.showCladeLabels && tree.isLabelSignificant(tree.getLabel(n)))
			{
				boolean overlap = insertAndReturnOverlap(n);
				if (!overlap)
				{
					decorator.lr.render(canvas, n, true);
				}
			}

			/*
			 * Do some extra stuff to clean up the thresholding artifacts.
			 */
			List l = tree.getChildrenOf(n);
			int sz = l.size();
			for (int i = 0; i < sz; i++)
			{
				PhyloNode child = (PhyloNode) l.get(i);
				NodeRange r = child.range;
				/*
				 * If this child is thresholded out, then draw a placemark line to its
				 * earliest or latest leaf node.
				 */
				if (!child.drawMe && child.isWithinScreen)
				{
					PhyloNode leaf = null;
					if (i == 0)
						leaf = (PhyloNode) tree.getFirstLeaf(child);
					else if (i == l.size() - 1)
						leaf = (PhyloNode) tree.getLastLeaf(child);
					else
						/*
						 * If this child is a "middle child", just do nothing.
						 */
						continue;
					decorator.lineRender.render(canvas, child, true);
					// drawLabel(leaf);
				}
			}
		}
		//		if (tree instanceof PhyloTree)
		//		{
		//			PhyloTree pt = (PhyloTree) tree;
		//			PhyloNode h = pt.hoveredNode;
		//			if (h != null && h.getParent() != null)
		//			{
		//				canvas.stroke(RenderConstants.hoverColor.getRGB());
		//				float weight = baseStroke * RenderConstants.hoverStroke;
		//				weight *= PhyloWidget.ui.traverser.glowTween.getPosition();
		//				canvas.strokeWeight(weight);
		//				canvas.fill(RenderConstants.hoverColor.getRGB());
		//				drawLineImpl((PhyloNode) h.getParent(), h);
		//				canvas.noStroke();
		//				canvas.fill(RenderConstants.hoverColor.getRGB());
		//				drawNodeMarkerImpl(h);
		//			}
		//		}
	}

	void hint()
	{
		Graphics2D g2 = ((PGraphicsJava2D) canvas).g2;
		oldRH = g2.getRenderingHints();
		if (PhyloWidget.cfg.antialias)
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		//		if (textSize > 100)
		//		{
		//			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		//					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		//			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		//					RenderingHints.VALUE_ANTIALIAS_OFF);
		//		}

	}

	boolean isAnyParentDrawn(PhyloNode n)
	{
		PhyloNode cur = (PhyloNode) n.getParent();
		while (cur != null)
		{
			if (cur.drawMe)
				return true;
			cur = (PhyloNode) cur.getParent();
		}
		return false;
	}

	Rectangle2D.Float rect1 = new Rectangle2D.Float();
	Rectangle2D.Float rect2 = new Rectangle2D.Float();
	Rectangle2D.Float rect3 = new Rectangle2D.Float();

	protected boolean isNodeWithinScreen(PhyloNode n)
	{
		/*
		 * Get this node range and set the rect.
		 */
		NodeRange r = n.range;
		//		Rectangle rect1 = new Rectangle();
		float EXPAND = 50;
		float EXPAND2 = 100;
		/*
		 * Try to get the parental noderange and set it.
		 */
		PhyloNode p = (PhyloNode) tree.getParentOf(n);
		rect1.x = r.loX - EXPAND;
		rect1.y = r.loY - EXPAND;
		rect1.width = r.hiX - r.loX + EXPAND2;
		rect1.height = r.hiY - r.loY + EXPAND2;

		if (p == null)
		{
			/*
			 * If we're the root node, just intersect this node's rect with the screen.
			 */
			return rect1.intersects(screenRect);
		} else
		{
			NodeRange r2 = p.range;
			/*
			 * Find the union of ourselves and our parent, and then intersect with screen.
			 * (This fixes the problem where a node is off the screen but we want its parent-line drawn.
			 */
			rect2.x = r2.loX - EXPAND;
			rect2.y = r2.loY - EXPAND;
			rect2.width = r2.hiX - r2.loX + EXPAND2;
			rect2.height = r2.hiY - r2.loY + EXPAND2;

			Rectangle.union(rect1, rect2, rect3);
			return screenRect.intersects(rect3);
		}
	}

	/**
	 * Updates this renderer's internal representation of the tree. This should
	 * only be called when the tree is changed.
	 */
	protected void layout()
	{
		if (!needsLayout)
			return;
		needsLayout = false;

		ArrayList<PhyloNode> ls = new ArrayList<PhyloNode>();
		ArrayList<PhyloNode> ns = new ArrayList<PhyloNode>();
		synchronized (this)
		{
			tree.getAll(tree.getRoot(), ls, ns);
			Thread.yield();

			leaves = new PhyloNode[ls.size()];
			nodes = new PhyloNode[ns.size()];
			leaves = ls.toArray(leaves);
			nodes = ns.toArray(nodes);
			/*
			 * Sort these nodes by significance (i.e. num of enclosed nodes).
			 */
			Arrays.sort(nodes, 0, nodes.length, tree.sorter);
			Thread.yield();
			/*
			 * Sort the leaves by "leaf" significance (first leaf = least depth to root)
			 */
			sigLeaves = new PhyloNode[leaves.length];
			for (int i = 0; i < leaves.length; i++)
			{
				sigLeaves[i] = leaves[i];
			}
			int dir = 1;
			if (PhyloWidget.cfg.prioritizeDistantLabels)
				dir = -1;
			Arrays.sort(sigLeaves, 0, sigLeaves.length, tree.new DepthToRootComparator(dir));
			Thread.yield();
		}

		/*
		 * Crate new nodeRange objects for this layout.
		 */
		synchronized (list)
		{
			list.clear();
			for (int i = 0; i < nodes.length; i++)
			{
				PhyloNode n = (PhyloNode) nodes[i];
				n.range.render = this;
				list.insert(n.range, false);
			}
			list.sortFull();
		}
		Thread.yield();

		/*
		 * ASSUMPTION: the leaves ArrayList contains a "sorted" view of the
		 * tree's leaves, i.e. in the correct ordering from top to bottom.
		 */
		FontMetrics fm = canvas.g2.getFontMetrics(font.font);
		for (int i = 0; i < nodes.length; i++)
		{
			PhyloNode n = nodes[i];
			/*
			 * If we have NHX annotations, put our species into the colors map.
			 * This is done within this loop just to save the effort of looping through all
			 * nodes again during layout.
			 */
			if (n.isNHX() && PhyloWidget.cfg.colorSpecies)
			{
				String tax = n.getAnnotation(TAXON_ID);
				if (tax != null)
				{
					decorator.taxonColorMap.put(tax, null);
				} else
				{
					String spec = n.getAnnotation(SPECIES_NAME);
					if (spec != null)
						decorator.taxonColorMap.put(spec, null);
				}
			}
			//			Graphics2D g2 = ((PGraphicsJava2D) canvas).g2;
			//				width = (float) fm.getStringBounds(n.getLabel(), g2).getWidth() / 100f;
			float width = UIUtils.getTextWidth(canvas, font, 100, n.getLabel(), true) / 100f;
			n.unitTextWidth = width;
		}

		Thread.yield();

		if (PhyloWidget.cfg.colorSpecies)
		{
			decorator.getColorsForSpeciesMap();
		}
		treeLayout.layout(tree, leaves, nodes);
	}

	public void layoutTrigger()
	{
		needsLayout = true;
	}

	public void nodesInRange(ArrayList arr, Rectangle2D.Float rect)
	{
		synchronized (list)
		{
			list.getInRange(arr, rect);
		}
	}

	public void positionText(PhyloNode n, TextField tf)
	{
		decorator.lr.positionText(this, n, tf);
	}

	protected void recalc()
	{
		rowSize = rect.height / leaves.length;
		textSize = rowSize * 0.9f;

		dotWidth = getNormalLineWidth() * PhyloWidget.cfg.nodeSize;
		//		rad = dotWidth / 2;

		//		System.out.println(rect);

		//		if (rowSize == rect.height)
		//			scaleX = 0;

		scaleX = rect.width;
		scaleY = rect.height;
		float scale = (float) Math.min(scaleX, scaleY);
		scaleX = scaleY = scale;

		// If we have few nodes, don't fill it up so much.
		if (leaves.length <= 10)
		{
			float scaleMult = 0.025f;
			float scaleFactor = scaleMult + (leaves.length) * ((1 - scaleMult) / 10);
			scaleX *= scaleFactor;
			scaleY *= scaleFactor;
			rowSize *= scaleFactor;
			textSize *= scaleFactor;
			dotWidth *= scaleFactor;
		}

		dx = (rect.width - scaleX) / 2;
		dy = (rect.height - scaleY) / 2;
		dx += rect.getX();
		dy += rect.getY();

		dFont = (font.ascent() - font.descent()) * textSize / 2;
		//		System.out.println("Scale:"+scaleX+"  "+scaleY);
		//		System.out.println("d:"+dx+"  "+dy);
	}

	//	public float getBranchLengthScaling()
	//	{
	//		return PhyloWidget.cfg.branchLengthScaling;
	//	}

	public void render(PGraphics canvas, float x, float y, float w, float h, boolean mainRender)
	{
		this.mainRender = mainRender;
		rect.setRect(x, y, w, h);
		if (tree == null)
			return;
		if (PhyloWidget.cfg.useDoubleBuffering)
		{
			drawToCanvas(canvas);
		} else
		{
			synchronized (tree)
			{
				this.canvas = (PGraphicsJava2D) canvas;
				layout();
				recalc();
				draw();
			}
		}
		framesToSwitch--;
	}

	//		this.rect.setFrame(x, y, w, h);
	//		this.canvas = canvas;
	////		canvas.background(0,0);
	//		if (tree == null)
	//			return;
	////		synchronized (this)
	////		{
	//			layout();
	//			recalc();
	//			draw();
	////		}
	//	}

	public void drawToBuffer(PGraphics g)
	{
		this.canvas = (PGraphicsJava2D) g;
		g.background(0, 0);
		/*
		 * All operations requiring integrity of the tree structure should synchronize on the tree object!
		 */
		synchronized (tree)
		{
			layout();
			recalc();
			draw();
		}
		this.canvas = null;
	}

	//	public UIRectangle getVisibleRect()
	//	{
	//		float rx = (float) dx;
	//		float ry = (float) (dy + (overhang < 0 ? overhang * textSize : 0));
	//		float sx = (float) (scaleX + biggestAspectRatio * textSize + dotWidth * 2);
	//		float sy = (float) (scaleY + Math.abs(overhang * textSize));
	//		return new UIRectangle(rx, ry, sx, sy);
	//	}

	Point mousePt = new Point();

	private float overhang;

	public void setMouseLocation(Point pt)
	{
		mousePt.setLocation(pt);
	}

	protected void setOptions()
	{
	}

	public void setTree(RootedTree t)
	{
		if (t == null)
			return;
		if (tree != null)
		{
			synchronized (tree)
			{
				tree.removeGraphListener(this);
				tree.dispose();
				tree = null;
			}
		}
		synchronized (t)
		{
			tree = t;
			tree.addGraphListener(this);
			needsLayout = true;
			fforwardMe = true;
		}
	}

	void unhint()
	{
		//		if (textSize > 100 && UIUtils.isJava2D(canvas))
		//		{
		Graphics2D g2 = ((PGraphicsJava2D) canvas).g2;
		g2.setRenderingHints(oldRH);
		//		}
	}

	private Point2D.Float tempPt = new Point2D.Float();

	/*
	 * This is called once per render.
	 */
	protected void updateNode(PhyloNode n)
	{
		/*
		 * Update the node's Tween.
		 */
		if (mainRender)
			n.update();

		/*
		 * Set the node's cached "real" x and y values.
		 */
		n.setRealX(calcRealX(n));
		n.setRealY(calcRealY(n));

		/*
		 * Update the nodeRange.
		 */
		NodeRange r = n.range;

		decorator.setCornerPoints(this, n);
		//		if (r == null)
		//			return;

		//		float[][] points = decorator.getCornerPoints(this, n, textSize);

		//		float minX, maxX, minY, maxY;
		//		minX = minY = Float.MAX_VALUE;
		//		maxX = maxY = Float.MIN_VALUE;
		//		for (float[] xy : points)
		//		{
		//			float x = xy[0];
		//			float y = xy[1];
		//
		//			if (x < minX)
		//				minX = x;
		//			if (x > maxX)
		//				maxX = x;
		//			if (y < minY)
		//				minY = y;
		//			if (y > maxY)
		//				maxY = y;
		//		}
		if (n.rect.getWidth() == 0)
		{
			n.rect.setFrame(n.getRealX(), n.getRealY(), 0, 0);
		}
		//		tempPt.setLocation(n.getRealX(), n.getRealY());
		//		n.rect.add(tempPt);

		r.loX = (float) n.rect.getMinX();
		r.hiX = (float) n.rect.getMaxX();
		r.loY = (float) n.rect.getMinY();
		r.hiY = (float) n.rect.getMaxY();

		//		System.out.println(n.rect);

		//		float realTextSize = textSize * n.zoomTextSize;
		//		r.loX = getX(n) - dotWidth / 2;
		//		float textHeight = (font.ascent() + font.descent()) * realTextSize;
		////		float textHeight = getRowHeight();
		//		r.loY = getY(n) - textHeight / 2;
		//		r.hiY = getY(n) + textHeight / 2;
		//		float textWidth = (float) n.aspectRatio * textSize;
		////		r.hiX = getX(n) + dotWidth / 2 + textWidth;
		//		r.hiX = getX(n) + (float)n.aspectRatio * getRowHeight();
		//		n.lastTextSize = realTextSize;
	}

	/**
	 * Notifies that a vertex has been added to the tree.
	 * 
	 * @param e
	 *            the vertex event.
	 */
	public void vertexAdded(GraphVertexChangeEvent e)
	{
		needsLayout = true;
	}

	/**
	 * Notifies that a vertex has been removed from the tree.
	 * 
	 * @param e
	 *            the vertex event.
	 */
	public void vertexRemoved(GraphVertexChangeEvent e)
	{
		needsLayout = true;
	}

	private LayoutBase oldLayout = null;
	private int framesToSwitch = 0;

	public void setLayout(LayoutBase layout)
	{
		this.oldLayout = this.treeLayout;
		this.treeLayout = layout;
		layoutTrigger();
		framesToSwitch = (int) PhyloWidget.cfg.animationFrames / 2;
	}

	public LayoutBase getTreeLayout()
	{
		//		if (framesToSwitch > 0)
		//			return oldLayout;
		//		else
		return treeLayout;
	}

	public void forceLayout()
	{
		needsLayout = true;
		layoutTrigger();
	}

}
