WFSUnitGUI {
	
	var <unit;
	
	var <parent, <composite, <views, <controller;
	var <viewHeight = 16, <labelWidth = 50;
	var <>action;
	
	*new { |parent, bounds, unit|
		^super.newCopyArgs( unit ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		if( parent.isNil ) { parent = Window( unit.defName ).front };
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		bounds.height = (margin.y * 2) + 
			( unit.argNames.size * (viewHeight + gap.y) ) - gap.y;
		
		controller = SimpleController( unit );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		views = ();
		
		unit.args.pairsDo({ |key, value, i|
			var vw;
			
			vw = ObjectView( composite, nil, unit, key, 
					unit.def.specs[i/2], controller );
			
			vw.action = { action.value( this, key, value ); };
			
			views[ key ] = vw;
		
		});
	}
	
	resize_ { |resize| composite.resize_(resize) }
	reset { unit.reset }
	
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

+ WFSUnit {
	gui { |parent, bounds| ^WFSUnitGUI( parent, bounds, this ) }
}