package groovyfx

import static groovyx.javafx.GroovyFX.start

start { application ->

    def fxml = new javafx.fxml.FXMLLoader(this.class.getResource('/groovyfx.fxml'))
    fxml.controller = new Controller(application: application)

    stage(title: 'G* Advent Calendar 2012', visible: true) {
        scene(root: fxml.load(), width: 800, height: 640) 
    }
}
