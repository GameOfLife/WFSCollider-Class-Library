ObjectView {
	
	classvar <viewHeight = 14;
	
	var <object, <key, <spec, <parent, <composite, <views; 
	var <>action;
		// views can be anything; i.e. the output of the makeFunc in the def
	
	*new { |parent, bounds, object, key, spec, controller|
		^super.newCopyArgs( object, key, spec ).init( parent, bounds, controller );	}
		
	init { |inParent, bounds, controller|
		var margin = 0, value;
		parent = inParent;
		if( parent.isNil ) { parent = Window( "%:%".format(object) ).front.decorate };
		
		if( bounds.isNil ) { 
			if( parent.asView.decorator.notNil )
				{ margin = parent.asView.decorator.margin.x; };
			bounds = parent.bounds.width - (2 * margin);
		};
		
		spec = (spec ?? {
			spec = (key.asSpec ? [0,1]).asSpec;
		}).asSpec;
		
		// spec = spec.adaptFromObject( object.perform( key ) );
		
		if( bounds.isNumber ) { bounds = 
			(bounds @ ((spec.viewNumLines * viewHeight) + ((spec.viewNumLines-1) * 4))) 
		};
		this.makeView( bounds, controller );
	}
	
	makeView { | bounds, controller |
		var createdController = false, setter;
		
		controller = controller ?? { 
			createdController = true; 
			SimpleController( object ); 
		};
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.onClose = { 
			controller.put( key, nil );
			if( createdController ) { controller.remove };
		};
		
		setter = key.asSetter;
		
		views = spec.makeView( composite, bounds.asRect.moveTo(0,0), key, 
				{ |vw, value| 
					object.perform( key.asSetter, value ); 
					action.value( this, value ); 
				}, 5 );
			
		this.update;
		
		controller.put( key, { |obj, key, value| spec.setView( views, value, false ) });
	}
	
	update {
		spec.setView( views, object.perform( key ) ); 
	}
	
	remove { composite.remove }
	
	resize_ { |val| composite.resize_(val); }
	
}