<html>
    <head>
        <link rel="stylesheet" href="/static/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="/static/css/mdb.min.css"/>
        <link rel="stylesheet" href="/static/css/style.css"/>
        <script>
            let ws = new WebSocket("ws://localhost:5000/chat");
            var socketId;
            ws.onopen = function(e){
                //
            };

            ws.onmessage = function(event){
                let msgData = JSON.parse(event.data);

                if(msgData.type == -1){
                    //if a new user has joined we will add it to our div area indicating all users
                    console.log(msgData.data)
                }
                else if(msgData.type === 0){
                    // initial handshake type, we will get the socketId, set our var and then send a message back with our username
                    socketId = msgData.data.id;
                    ws.send(JSON.stringify({"type":0,"data":{"id":socketId,"username":"${room.username}"}}));
                } else if(msgData.type === 1){
                    // if we get type 1, we have received a new message in our chat room, we will add the card with the message details to our page
                    let msgBoard = document.getElementById("msgboard");
                    let newMsg =  document.createElement("div");
                    newMsg.innerHTML = "<div class='fade-in card my-2'>" +
                        "<div class='card-body'><h5>"+msgData.content+"</h5> </div> " +
                            "<div class='card-footer d-flex justify-content-between'> "+
                                "<h6 class='text-muted'>"+msgData.username+"</h6>"+
                                "<p class='text-muted'>"+msgData.msgTimestamp+"</p>"+
                            "</div>"+
                        "</div>";
                    msgBoard.prepend(newMsg);

                }
            };

            function sendMessage(){
                let msg = document.getElementById('msg');
                let author = document.getElementById('username').value;

                ws.send(JSON.stringify({type:1,data:{username:author,content:msg.value,roomName:"${room.name}"}})); // send type 1(new msg), and our username,the message, room name to server
                msg.value = ""; // clear the message box after sending the data
            }
        </script>
        <title>KitChat - ${room.name}</title>
    </head>

    <body>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/popper.js"></script>
        <script type="text/javascript" src="/static/js/bootstrap.js"></script>
        <script type="text/javascript" src="/static/js/mdb.js"></script>

        <div class="container justify-content-center">
            <h2 class="font-weight-bold my-5">Chat Room - ${room.name}</h2>

        <div id="msgboard" class="my-5">


        </div>
        <form class="my-5" >
            <div class="form-group">

                <textarea id="msg" name="msg" rows="5" cols="100"></textarea>
            </div>

            <div class="form-group">
                <p>Chatting as ${room.username}</p>
                <input hidden id="username" name="username" value=${room.username} />
            </div>
            <div class="form-group">
                <button type="button" onclick="sendMessage()" class="btn btn-brown">Send</button>
            </div>
        </form>
        </div>
    </body>
</html>