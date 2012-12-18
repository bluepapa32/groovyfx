package groovyfx

import javafx.application.Application
import javafx.beans.value.ChangeListener
import javafx.concurrent.*
import javafx.concurrent.Worker.State
import javafx.event.*
import javafx.fxml.*
import javafx.scene.control.*
import javafx.scene.image.*
import javafx.scene.layout.*
import javafx.scene.web.*

import groovy.xml.dom.DOMCategory

import org.w3c.dom.events.EventListener

class Controller implements Initializable {

    @FXML
    GridPane calendar

    @FXML
    StackPane webViewPane

    @FXML
    WebView webView

    Application application

    def users
    def items

    def onNameClicked(ActionEvent e) {
        def name = e.source.text
        def user = users.find { it.name == name }
        def item = items.find { it.author == name }
        webView.engine.loadContent("""<html>
<body>
<img src="${user.icon}" /> ${name}<br>
<p>${item.description}</p>
<p>${item.pubDate}</p>
</body>
</html>""")
        webViewPane.visible = true
    }

    def onDateClicked(Event e) {
        webView.engine.loadContent("")
        webViewPane.visible = false
    }

    def onWebViewClicked(Event e) { e.consume() }

    void initialize(URL url, ResourceBundle bundle) {

        //
        calendar.children.each { node ->
            node.prefWidthProperty().bind(calendar.widthProperty().divide(7))
        }

        //
        webView.contextMenuEnabled = false
        webView.engine.loadWorker.stateProperty().addListener({ ov, oldState, newState ->
            if (newState == State.SUCCEEDED) {

                def root = webView.engine.document.documentElement
                use(DOMCategory) {
                    root.'**'.'A'.each { e ->
                        def event = { ev -> 
                            application.hostServices.showDocument(e.href)
                            ev.preventDefault()
                        } as EventListener
                        e.addEventListener("click", event, false)
                    }
                }

            }
        } as ChangeListener)

        //
        Service service = [
            createTask: { -> [
                call: { ->
                    def xml = new XmlSlurper().parse('http://api.atnd.org/events/users/?event_id=34317')
                    users = xml.events.event.users.user.collect { user -> [
                        name: user.nickname.text(),
                        icon: user.twitter_img.text()
                    ]}
                    users = users[1..-1] + users[0]

                    def rss = new XmlSlurper().parse('http://atnd.org/comments/34317.rss')
                    items = rss.channel.item.collect { item -> [
                        author:      item.author.text(),
                        description: item.description.text().replaceAll('(?<!")(https?://[^ "<>]+)(?!")', '<a href="$1">$1</a>'),
                        pubDate:     item.pubDate.text()
                    ]}

                    return null
                }
            ] as Task }
        ] as Service

        service.onSucceeded = { e ->
            calendar.lookupAll('.name')
                    .findAll { it instanceof Hyperlink }
                    .eachWithIndex { node, index ->
                node.text = users[index]?.name
            }

            calendar.lookupAll('.icon')
                    .eachWithIndex { node, index ->
                def icon = users[index]?.icon
                node.image = icon ? new Image(icon, 30.0, 30.0, true, true, true) : null
            }

        } as EventHandler
 
        service.start()
    }
}
