<html>
    <head>
        <title>Kit Chat - Sign Up</title>
    </head>

    <body>
        <#include "menu.ftl">

        <div class="container">
            <h2 class="text-center font-weight-bold my-3">Sign Up</h2>

            <form class="my-4" action="/signup" method="post">
                <div class="form-group">
                    <input class="form-control" type="text" id="username" name="username" placeholder="Username"/>
                </div>

                <div class="form-group">
                    <input class="form-control" type="password" id="password" name="password" placeholder="Password"/>
                </div>

                <div class="form-group">
                    <input class="form-control" type="password" id="verifypassword" name="verifypassword" placeholder="Re-Enter Password"/>
                </div>

                <div class="form-group">
                    <input type="submit" class="btn btn-brown" value="Submit"/>
                </div>
            </form>
        </div>

    </body>
</html>