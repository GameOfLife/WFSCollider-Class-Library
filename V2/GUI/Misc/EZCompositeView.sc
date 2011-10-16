EZCompositeView : EZGui {
	
	classvar <>defaultBounds;
	
	*initClass {
		defaultBounds = 350@20;
	}
	
	*new { |parent, bounds, addFlowLayout = true, gap, margin|
		^super.new.init( parent, bounds, addFlowLayout, gap, margin );
	}
	
	init { |parentView, bounds, addFlowLayout, argGap, argMargin|
		
		var windowName;
		
		if( parentView.isString ) {
			windowName = parentView;
			parentView = nil;
		};
		
		this.prMakeMarginGap(parentView, argMargin, argGap);

		bounds.isNil.if{ bounds = defaultBounds };

		// if no parent, then pop up window
		# view,bounds = this.prMakeView( parentView,bounds);
		
		if( windowName.notNil ) {
			this.setWindowName( windowName );
		};
		
		if( addFlowLayout ) { this.addFlowLayout };
	}
	
	addFlowLayout { |argMargin, argGap|
		view.addFlowLayout( argMargin ? margin, argMargin? gap );
	}
	
	decorator { ^view.decorator }
	
	
	
	asView { ^view }
	
	add { |aView| view.add(aView) }
	
	resize { ^view.resize }
	resize_ { |resize| view.resize = resize }
	
	onClose { ^view.onClose }
	onClose_ { |func| view.onClose = func; }
}

+ EZGui {
	
	findWindow { ^view.getParents.last.findWindow }
	
	setWindowName { |name|
		this.findWindow.name = name ? "";
	}
	
	front { this.findWindow.front }
	isClosed { ^view.isClosed }
	
}