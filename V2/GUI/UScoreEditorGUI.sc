UServerCenter{
    classvar <>servers;

    *initClass {
        servers = [Server.default]
    }
}

UScoreEditorGUI {
	
	classvar <>current, <all;

	var <scoreEditor;

	var <>scoreView, <>window;
	var <usessionMouseEventsManager;

	//*initClass { UI.registerForShutdown({ scoreEditor.askForSave = false }); }

	*new { |scoreEditor|
		^super.newCopyArgs( scoreEditor)
			.addToAll
			.newWindow
	}

	toFront {
	    if( window.isClosed.not ) {
	     window.front;
	    };
	}

	addToAll {
		all = all.asCollection.add( this );
	}

	removeFromAll { if( all.notNil ) { all.remove( this ); }; }

	newWindow {

		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle, margin, gap, topBarH, tranBarH, view, centerView, centerBounds;
        var bounds = Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300);

        window = Window("Score Editor", bounds).front;

        margin = 4;
        gap = 2;
        topBarH = 22;
        tranBarH = 22;
        view = window.view;
        view.background_( Color.grey(0.5) );
        view.addFlowLayout(margin@margin,gap@gap);
        view.resize_(5);

        centerBounds = Rect(0,0, 680-8, 300-( topBarH + tranBarH + (2*margin) + (2*gap) ));
        //centerView = CompositeView(view, centerBounds).resize_(5);
        scoreView = UScoreView(view, centerBounds, scoreEditor );
        
        //TOP
        UScoreEditorGui_TopBar(view,Rect(0,0, bounds.width-(2*margin), topBarH ),scoreView);
        view.decorator.nextLine;
        
        //CENTER
        scoreView.makeView;
        view.decorator.nextLine;
        
        //BOTTOM
        UScoreEditorGui_TransportBar(view,  Rect(0,0, bounds.width - (2*margin), tranBarH ), scoreView);
	}
}		