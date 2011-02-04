TransformGUI {
	
	var <transform;
	
	var <parent, <composite, <views, <controller;
	var <viewHeight = 16, <labelWidth = 50;
	var <>action;
	
	*new { |parent, bounds, transform|
		^super.newCopyArgs( transform ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		if( parent.isNil ) { parent = Window( transform.defName ).front };
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		bounds.height = (margin.y * 2) + 
			( transform.argNames.size * (viewHeight + gap.y) ) - gap.y;
		
		controller = SimpleController( transform );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		views = ();
		
		transform.args.pairsDo({ |key, value|
			var vw;
			
			vw = ObjectView( composite, nil, transform, key, 
					transform.def.specs[key], nil, controller );
			
			vw.action = { action.value( this, key, value ); };
			
			views[ key ] = vw;
		
		});
	}
	
	resize_ { |resize| composite.resize_(resize) }
	reset { transform.reset }
	
	font_ { |font| views.values.do({ |vw| vw.font = font }); }
	viewHeight_ { |height = 16|
		views.values.do({ |vw| vw.view.bounds = vw.view.bounds.height_( height ) });
		composite.decorator.reFlow( composite );
	}
	labelWidth_ { |width=50|
		labelWidth = width;
		views.values.do(_.labelWidth_(width));
	}
	
	view { ^composite }
	
}

+ Transform {
	gui { |parent, bounds| ^TransformGUI( parent, bounds, this ) }
}
