UArchivable {

	var <>filePath, <lastVersionSaved;

    isDirty{
	    ^lastVersionSaved !! { |x| this.asTextArchive !=  x} ? true
	}

    archiveAsCompileString { ^true }

    // Subclasses need to implement this to get all the goodies !
    getInitArgs { this.subclassResponsibility(thisMethod) }

    *readTextArchive { |pathname|
	    var res, sub;
	    res = pathname.load;
	    sub = this.subclasses;
	    sub = sub ? [];
	    if( res.class == this or: { this.subclasses.includes( res.class ) } ) {
	        res.filePath_(pathname);
		   ^res;
	    } {
		    "%:readTextArchive - wrong type (%)\n".postf( this, res );
		    ^nil;
	    }
    }

    readTextArchive { |pathname|
	    var res;
	    res = this.class.readTextArchive( pathname );
	    if( res.notNil ) {
		    this.init( res.getInitArgs );
	    };
	    filePath = pathname;
    }

    write { |path, overwrite=false, ask=true, successAction, cancelAction|
	    var writeFunc;
	    writeFunc = { |overwrite, ask, path|
		    var text;
		    text = this.asTextArchive;
		    File.checkDo( path, { |f|
				f.write( text );
			}, overwrite, ask);
			successAction.value(path);
	    };

	    if( path.isNil ) {
		    Dialog.savePanel( { |pth|
			    path = pth;
			    writeFunc.value(true,false,path);
		    }, cancelAction );
	    } {
		    writeFunc.value(overwrite,ask,path);
	    };
    }

    read { |path, action|
         var score;

        if( path.isNil ) {
		    Dialog.getPaths( { |paths|
	             this.readTextArchive( paths[0] );
	             action.value(score);
	        });
	    } {
	            path = path.standardizePath;
	            this.readTextArchive( path );
	            action.value(score);
	    };
    }

    *read { |path, action|
        var score;

        if( path.isNil ) {
		    Dialog.getPaths( { |paths|
	             score = this.readTextArchive( paths[0] );
	             action.value(score);
	             score
	        });
	    } {
	            path = path.standardizePath;
	            score = this.readTextArchive( path );
	            action.value(score);
	            ^score
	    };
    }

    save { |successAction, cancelAction|
	    if(this.isDirty){
            filePath !! { |x| this.write(x,true, true,
                { |x| filePath = x; lastVersionSaved = this.asTextArchive; this.onSaveAction; successAction.value}, cancelAction) } ?? {
                this.saveAs(nil,successAction, cancelAction)
            }
        }
	}

	saveAs { |path, successAction, cancelAction|
	    this.write(path, true, true,
	        { |x| filePath = x; lastVersionSaved = this.asTextArchive; this.onSaveAction; successAction.value}, cancelAction)
	}

	onSaveAction{ this.subclassResponsibility(thisMethod) }

}