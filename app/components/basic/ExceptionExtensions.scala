package components.basic

extension (error: Exception)
    def serializeForLog: String =
        error.getMessage() + "\n" + error.getStackTrace().mkString("\n")
