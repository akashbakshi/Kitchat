<html>
    <head>
        <title>Kit Chat - Log In</title>
    </head>

    <body>
        <#include "menu.ftl">

        <div class="container">
            <#if (error??)>
                <div class="alert alert-danger my-2" role="alert">
                   Invalid Username or Password Combination
                </div>
            </#if>
            <h2 class="text-center font-weight-bold my-3">Log In</h2>

            <form class="my-4" action="/login" method="post">
                <div class="form-group">
                    <input class="form-control" type="text" id="username" name="username" placeholder="Username"/>
                </div>

                <div class="form-group">
                    <input class="form-control" type="password" id="password" name="password" placeholder="Password"/>
                </div>

                <div class="form-group">
                    <input type="submit" class="btn btn-brown" value="Log In"/>
                </div>
            </form>
        </div>

    </body>
</html>