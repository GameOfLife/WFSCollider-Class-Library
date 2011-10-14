FuncView {
	var <view, <dragBoth, <labelView, <editButton;
	
	*new { |parent, bounds, func, label, labelWidth, labelHeight|
		^super.newCopyArgs( func ? {} )
			.init( parent, bounds, label, labelWidth ? 60 ,labelHeight ? 20 );
	}
	
	init { |parent, bounds, label, labelWidth, labelHeight|
		var margin, gap, cs, hasParent = false;
		if( parent.notNil ) { hasParent = true };
		#view, bounds, margin, gap = EZGui.makeParentView( parent, bounds ? (350@labelHeight) );
		view.addFlowLayout( 0@0, gap );
		if( label.notNil ) {
			if( hasParent ) {
				labelView = StaticText( view, bounds.width @ labelHeight )
					.string_( label );
				bounds.height = bounds.height - ( labelHeight + gap.y );
			} {
				view.parent.findWindow.name = label;
			};
		};
	}

	
}

FuncEditView {
	
	var <>func, <cs;
	var <view, <textView, <okButton, <cancelButton, <revertButton, <labelView;
	var <>action, <>failAction, <>cancelAction;
	
	*new { |parent, bounds, func, label, labelHeight = 20|
		^super.newCopyArgs( func ? {} ).init( parent, bounds, label, labelHeight );
	}
	
	init { |parent, bounds, label, labelHeight|
		var margin, gap, cs, hasParent = false;
		if( parent.notNil ) { hasParent = true };
		#view, bounds, margin, gap = EZGui.makeParentView( parent, bounds ? (350@100) );
		view.addFlowLayout( 0@0, gap );
		if( label.notNil ) {
			if( hasParent ) {
				labelView = StaticText( view, bounds.width @ labelHeight )
					.string_( label );
				bounds.height = bounds.height - ( labelHeight + gap.y );
			} {
				view.parent.findWindow.name = label;
			};
		};
		textView = TextView( view, 
				bounds.width @ 
				(bounds.height-(gap.y+20)) )
			.resize_( 5 )
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(true)
			.autohidesScrollers_(true)
			.usesTabToFocusNextView_( false )
			.keyDownAction_( { |vw| vw.syntaxColorize; } )
			.keyUpAction_( { |vw| vw.syntaxColorize; this.checkChanged } );

		view.decorator.nextLine;
		view.decorator.shift( bounds.width - (3 * (60 + gap.x)) );
		
		cancelButton = SmoothButton( view,  60@20 )
			.label_( "cancel" )
			.canFocus_( false )
			.resize_( 9 )
			.action_({ cancelAction.value( this ); });
			
		revertButton = SmoothButton( view,  60@20 )
			.label_( "revert" )
			.canFocus_( false )
			.resize_( 9 )
			.action_({ this.setFunc( func ); });
			
		okButton = SmoothButton( view,  60@20 )
			.label_( "OK" )
			.canFocus_( false )
			.resize_( 9 )
			.action_( { 
				var fn;
				fn = { textView.string.interpret }.try;
				if( fn.notNil ) {
					func = fn;
					action.value( this );
				} {
					failAction.value( this );
				};
			} );
			
		this.setFunc( func );
	}
	
	setFunc { |function|
		cs = function.asCompileString;
		
		if( ( /*{*/ cs.last == $} ) && { cs[cs.size-2] != $\n }  ) {
			/*{*/ cs = cs[..cs.size-2] ++ "\n}";
		};
			
		textView.string = cs;
		textView.syntaxColorize;
		this.checkChanged;
	}
	
	checkChanged {
		if( textView.string != cs ) {
			revertButton.enabled = true;
		} {
			revertButton.enabled = false;
		};
	}
	
	resize_ { |resize| view.resize_(resize) }
	
	ok { okButton.doAction }
	cancel { cancelButton.doAction }
	revert { revertButton.doAction }
	
	font_ { |font|
		labelView !? { labelView.font = font };
		[ cancelButton, revertButton, okButton ].do(_.font_(font));
	}
}
