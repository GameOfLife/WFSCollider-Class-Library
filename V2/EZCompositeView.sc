EZCompositeView : EZGui {
	
	classvar <>defaultBounds;
	
	*initClass {
		defaultBounds = 350@20;
	}
	
	*new { |parent, bounds, addFlowLayout = true, gap, margin|
		^super.new.init( parent, bounds, addFlowLayout, gap, margin );
	}
	
	init { |parentView, bounds, addFlowLayout, argGap, argMargin|
		
		this.prMakeMarginGap(parentView, argMargin, argGap);

		bounds.isNil.if{ bounds = defaultBounds };

		// if no parent, then pop up window
		# view,bounds = this.prMakeView( parentView,bounds);
		
		if( addFlowLayout ) { this.addFlowLayout };
	}
	
	addFlowLayout { |argMargin, argGap|
		view.addFlowLayout( argMargin ? margin, argMargin? gap );
	}
	
	asView { ^view }
	
	add { |aView| view.add(aView) }
	
	resize { ^view.resize }
	resize_ { |resize| view.resize = resize }
	
	onClose { ^view.onClose }
	onClose_ { |func| view.onClose = func; }
}