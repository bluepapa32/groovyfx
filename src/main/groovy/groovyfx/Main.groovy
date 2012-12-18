package groovyfx

import static groovyx.javafx.GroovyFX.start

start {
    stage(title: 'G* Advent Calendar 2012', visible: true) {
        scene(width: 800, height: 640) {
            fxml this.class.getResource('/groovyfx.fxml')
        }
    }
}
