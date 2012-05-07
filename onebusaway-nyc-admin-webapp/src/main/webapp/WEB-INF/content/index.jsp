<%--

    Copyright (c) 2011 Metropolitan Transportation Authority

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.

--%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<html xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core">
<jsp:directive.page contentType="text/html" />
<head>
	<title>Admin Webapp</title>
</head>
<body>
	<h2>Welcome Admin</h2>
	
	<ul>
		<li>
		<s:url var="url" action="createuser"/>
		<s:a href="%{url}">Create User</s:a>
		</li>
		<li>
		<s:url var="url" action="createapikeys"/>
		<s:a href="%{url}">Create API key</s:a>
		</li>
	</ul>

</body>
</html>