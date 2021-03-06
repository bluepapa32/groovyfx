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
        new StringWriter().withWriter { w ->
            new groovy.xml.MarkupBuilder(w).html {
                head {}
                body {
                    img src: user.icon, width: 40, height: 40
                    span name
                    br()
                    p { mkp.yieldUnescaped item.description }
                    p item.pubDate
                }
            }
            webView.engine.loadContent("${w}")
        }
        webViewPane.visible = true
    }

    def onWebViewClicked(Event e) { e.consume() }

    def onWebViewBackGroundClicked(Event e) {
        webView.engine.loadContent("")
        webViewPane.visible = false
    }

    def onWebViewCloseButtonClicked(Event e) {
        onWebViewBackGroundClicked(e)
    }

    void initialize(URL url, ResourceBundle bundle) {
        initCalendar()
        initWebView()
    }

    def initCalendar() {
        calendar.children.each { node ->
            node.prefWidthProperty().bind(calendar.widthProperty().divide(7))
        }
        updateCalendar()
    }

    def initWebView() {
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
    }

    def updateCalendar() {
        def service = [
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
                        description: item.description.text().replaceAll('(?<!")(https?://[^ "<>]+)(?!")', '<p><a href="$1">$1</a></p>'),
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
                def name = users[index]?.name
                node.text = name
                node.disable = !items.find { it.author == name }
            }

            calendar.lookupAll('.icon')
                    .eachWithIndex { node, index ->
                def name = users[index]?.name
                def icon = users[index]?.icon
                node.image = icon ? new Image(icon, 30.0, 30.0, true, true, true) : null
                node.disable = !items.find { it.author == name }
            }

        } as EventHandler
 
        service.start()
    }
}
